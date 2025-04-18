package fi.oph.suorituspalvelu.business

import fi.oph.suorituspalvelu.business.Tietolahde.VIRTA
import org.skyscreamer.jsonassert.{JSONCompare, JSONCompareMode}
import slick.jdbc.{JdbcBackend, SQLActionBuilder}
import slick.jdbc.PostgresProfile.api.*

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import org.slf4j.LoggerFactory
import slick.sql.SqlStreamingAction

import java.time.Instant
import java.util.UUID

class KantaOperaatiot(db: JdbcBackend.JdbcDatabaseDef) {

  final val DB_TIMEOUT = 30.seconds
  val LOG = LoggerFactory.getLogger(classOf[KantaOperaatiot])

  def getUUID(): UUID =
    UUID.randomUUID()

  def sameAsExistingData(oppijaNumero: String, tietolahde: Tietolahde, data: String): Boolean =
    tietolahde match
      case Tietolahde.KOSKI | Tietolahde.YTR =>
        Await.result(db.run(sql"""
            SELECT data_json
            FROM versiot
            WHERE oppijanumero=${oppijaNumero} AND lahde=${tietolahde.toString}::lahde AND upper(voimassaolo)='infinity'::timestamptz
        """.as[String]), DB_TIMEOUT)
        .headOption
        .map(existingData => JSONCompare.compareJSON(existingData, data, JSONCompareMode.NON_EXTENSIBLE).passed())
        .getOrElse(false)
      case default => false

  def tallennaJarjestelmaVersio(oppijaNumero: String, tietolahde: Tietolahde, data: String): Option[VersioEntiteetti] =
    if(tietolahde==Tietolahde.VIRKAILIJA)
      throw new RuntimeException("Virkailijan versioita ei voi tallentaa tällä metodilla")

    if(sameAsExistingData(oppijaNumero, tietolahde, data)) // TODO: tämä pitää tehdä samassa transaktiossa kun on lukko oppijaan!
      None
    else
      val tunniste = getUUID()
      val timestamp = Instant.now
      val insertOppijaAction = sqlu"INSERT INTO oppijat(oppijanumero) VALUES (${oppijaNumero}) ON CONFLICT DO NOTHING"
      val lockOppijaAction = sql"""SELECT 1 FROM oppijat WHERE oppijanumero=${oppijaNumero} FOR UPDATE"""
      val discontinueOldVersionAction = sqlu"""UPDATE versiot SET voimassaolo=tstzrange(lower(voimassaolo), ${timestamp.toString}::timestamptz)"""
      val insertVersioAction = tietolahde match
        case VIRTA => sqlu"""
              INSERT INTO versiot(tunniste, oppijanumero, voimassaolo, lahde, data_json)
              VALUES(${tunniste.toString}::uuid, ${oppijaNumero}, tstzrange(${timestamp.toString}::timestamptz, 'infinity'::timestamptz), ${tietolahde.toString}::lahde, ${data}::xml)"""
        case default => sqlu"""
              INSERT INTO versiot(tunniste, oppijanumero, voimassaolo, lahde, data_json)
              VALUES(${tunniste.toString}::uuid, ${oppijaNumero}, tstzrange(${timestamp.toString}::timestamptz, 'infinity'::timestamptz), ${tietolahde.toString}::lahde, ${data}::jsonb)"""

      Await.result(db.run(DBIO.sequence(Seq(insertOppijaAction, lockOppijaAction.as[Int], discontinueOldVersionAction, insertVersioAction)).transactionally), DB_TIMEOUT)
      Some(VersioEntiteetti(tunniste, oppijaNumero, timestamp, None, tietolahde))

  def haeData(versio: VersioEntiteetti): String =
    Await.result(db.run(
        (versio.tietolahde match
          case VIRTA => sql"""
              SELECT data_xml
              FROM versiot
              WHERE tunniste=${versio.tunniste.toString}::UUID"""
          case default => sql"""
              SELECT data_json
              FROM versiot
              WHERE tunniste=${versio.tunniste.toString}::UUID"""
        ).as[String]), DB_TIMEOUT)
      .head

  def puraSuoritukset(versio: VersioEntiteetti, suoritus: Suoritus, parentTunniste: Option[UUID]): (GenericSuoritus, DBIOAction[_, NoStream, Effect]) =
    val tunniste = getUUID()
    val lapset = suoritus.osaSuoritukset.map(osasuoritus => puraSuoritukset(versio, osasuoritus, Some(tunniste)))
    parentTunniste match
      case Some(parent) => (GenericSuoritus(suoritus.tyyppi, lapset.map(lapsi => lapsi._1)), DBIO.sequence(Seq(sqlu"""INSERT INTO suoritukset(tunniste, parent_tunniste, tyyppi) VALUES(${tunniste.toString}::uuid, ${parentTunniste.get.toString}::uuid, ${suoritus.tyyppi})""") ++ lapset.map(lapsi => lapsi._2)))
      case None => (GenericSuoritus(suoritus.tyyppi, lapset.map(lapsi => lapsi._1)), DBIO.sequence(Seq(sqlu"""INSERT INTO suoritukset(tunniste, versio_tunniste, tyyppi) VALUES(${tunniste.toString}::uuid, ${versio.tunniste.toString}::uuid, ${suoritus.tyyppi})""") ++ lapset.map(lapsi => lapsi._2)))

  def poistaVersionSuoritukset(versio: VersioEntiteetti): DBIOAction[_, NoStream, Effect] =
    DBIO.sequence(Seq(
      sqlu"""
          WITH RECURSIVE
            parents(tunniste, parent_tunniste, path) AS (
              SELECT
                suoritukset.tunniste,
                suoritukset.parent_tunniste,
                ARRAY[]::uuid[]
              FROM suoritukset
              WHERE versio_tunniste=${versio.tunniste.toString}::uuid
            UNION
              SELECT
                s.tunniste,
                s.parent_tunniste,
                path || s.tunniste
              FROM suoritukset s, parents p
              WHERE p.tunniste=s.parent_tunniste
              AND (NOT (s.tunniste = ANY(path))) -- estetään syklit
          )
          DELETE FROM suoritukset USING parents WHERE parents.tunniste=suoritukset.tunniste;
       """))

  def tallennaSuoritukset(versio: VersioEntiteetti, juuriSuoritus: Suoritus): GenericSuoritus =
    val poistaSuoritukset = poistaVersionSuoritukset(versio)
    val (juuriSuoritusEntiteetti, inserts) = puraSuoritukset(versio, juuriSuoritus, None)
    Await.result(db.run(DBIO.sequence(Seq(poistaSuoritukset, inserts)).transactionally), DB_TIMEOUT)
    juuriSuoritusEntiteetti

  private def haeSuoritukset(versioTunnisteetQuery: slick.jdbc.SQLActionBuilder): Map[VersioEntiteetti, Seq[GenericSuoritus]] =
    var osaSuoritukset: Map[String, Seq[GenericSuoritus]] = Map.empty
    Await.result(db.run(
        (sql"""
          WITH RECURSIVE
            versioTunnisteet AS (SELECT tunniste FROM ("""
          concat
            versioTunnisteetQuery
          concat sql""") AS versioTunnisteet(tunniste)),
            parents(tunniste, parent_tunniste, path) AS (
              SELECT
                suoritukset.tunniste,
                suoritukset.parent_tunniste,
                ARRAY[]::uuid[]
              FROM suoritukset
              INNER JOIN versioTunnisteet ON versioTunnisteet.tunniste=suoritukset.versio_tunniste
            UNION
              SELECT
                s.tunniste,
                s.parent_tunniste,
                p.path || s.tunniste
              FROM suoritukset s, parents p
              WHERE p.tunniste=s.parent_tunniste
              AND (NOT (s.tunniste = ANY(path))) -- estetään syklit
          )
          SELECT
            suoritukset.tunniste,
            suoritukset.parent_tunniste,
            suoritukset.tyyppi,
            versiot.tunniste,
            versiot.oppijanumero,
            to_json(lower(versiot.voimassaolo)::timestamptz)#>>'{}',
            to_json(upper(versiot.voimassaolo)::timestamptz)#>>'{}',
            versiot.lahde,
            versiot.hakuoid
          FROM parents
          JOIN suoritukset ON parents.tunniste=suoritukset.tunniste
          LEFT JOIN versiot ON suoritukset.versio_tunniste=versiot.tunniste
          ORDER BY path DESC;
        """).as[(String, String, String, String, String, String, String, String, String)]), DB_TIMEOUT)
      .map((tunniste, parentTunniste, koodiArvo, versioTunniste, oppijaNumero, alku, loppu, tietoLahde, hakuOid) =>
        val suoritusEntiteetti = GenericSuoritus(koodiArvo, osaSuoritukset.getOrElse(tunniste, Seq.empty))

        osaSuoritukset = osaSuoritukset.updatedWith(parentTunniste)(osaSuoritukset => osaSuoritukset match
          case Some(osaSuoritukset) => Some(suoritusEntiteetti +: osaSuoritukset)
          case None => Some(Seq(suoritusEntiteetti))
        )

        (Option(versioTunniste), Option(parentTunniste)) match
          // palautetaan vain juurisuoritusentiteetit mapattuna versioihin
          case (Some(versioTunniste), None) => Some(VersioEntiteetti(
            UUID.fromString(versioTunniste),
            oppijaNumero,
            Instant.parse(alku),
            loppu match
              case "infinity" => None
              case default => Some(Instant.parse(loppu)),
            Tietolahde.valueOf(tietoLahde)
          ) -> Seq(suoritusEntiteetti))
          case (Some(_), Some(_)) => throw new RuntimeException()
          case (None, None)       => throw new RuntimeException()
          case (None, Some(_))    => None
      )
      .flatten
      .toMap

  def haeSuoritukset(oppijaNumero: String): Map[VersioEntiteetti, Seq[GenericSuoritus]] =
    haeSuoritukset(sql"""SELECT tunniste FROM versiot WHERE oppijanumero=${oppijaNumero} AND upper(voimassaolo)='infinity'::timestamptz""")
}

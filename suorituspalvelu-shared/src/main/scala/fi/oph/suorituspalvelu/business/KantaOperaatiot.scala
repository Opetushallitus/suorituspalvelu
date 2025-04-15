package fi.oph.suorituspalvelu.business

import fi.oph.suorituspalvelu.business.Tietolahde.VIRTA
import slick.jdbc.JdbcBackend
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

  def tallennaVersio(oppijaNumero: String, tietolahde: Tietolahde, data: String, hakuOid: Option[String]): VersioEntiteetti =
    val tunniste = getUUID()
    val timestamp = Instant.now
    val discontinueOldVersionAction = sqlu"""UPDATE versiot SET voimassaolo=tstzrange(lower(voimassaolo), ${timestamp.toString}::timestamptz)"""
    val insertVersioAction = tietolahde match
      case VIRTA => sqlu"""
            INSERT INTO versiot(tunniste, oppijanumero, voimassaolo, lahde, data_json, hakuoid)
            VALUES(${tunniste.toString}::uuid, ${oppijaNumero}, tstzrange(${timestamp.toString}::timestamptz, 'infinity'::timestamptz), ${tietolahde.toString}::lahde, ${data}::xml, ${hakuOid})"""
      case default => sqlu"""
            INSERT INTO versiot(tunniste, oppijanumero, voimassaolo, lahde, data_json, hakuoid)
            VALUES(${tunniste.toString}::uuid, ${oppijaNumero}, tstzrange(${timestamp.toString}::timestamptz, 'infinity'::timestamptz), ${tietolahde.toString}::lahde, ${data}::jsonb, ${hakuOid})"""

    Await.result(db.run(DBIO.sequence(Seq(discontinueOldVersionAction, insertVersioAction)).transactionally), DB_TIMEOUT)
    VersioEntiteetti(tunniste, oppijaNumero, timestamp, None, tietolahde)

  def puraSuoritukset(versio: VersioEntiteetti, suoritus: Suoritus, parentTunniste: Option[UUID]): (SuoritusEntiteetti, DBIOAction[_, NoStream, Effect]) =
    val tunniste = getUUID()
    val lapset = suoritus.osaSuoritukset.map(osasuoritus => puraSuoritukset(versio, osasuoritus, Some(tunniste)))
    parentTunniste match
      case Some(parent) => (SuoritusEntiteetti(tunniste, suoritus.koodiArvo, lapset.map(lapsi => lapsi._1)), DBIO.sequence(Seq(sqlu"""INSERT INTO suoritukset(tunniste, parent_tunniste, koodiarvo) VALUES(${tunniste.toString}::uuid, ${parentTunniste.get.toString}::uuid, ${suoritus.koodiArvo})""") ++ lapset.map(lapsi => lapsi._2)))
      case None => (SuoritusEntiteetti(tunniste, suoritus.koodiArvo, lapset.map(lapsi => lapsi._1)), DBIO.sequence(Seq(sqlu"""INSERT INTO suoritukset(tunniste, versio_tunniste, koodiarvo) VALUES(${tunniste.toString}::uuid, ${versio.tunniste.toString}::uuid, ${suoritus.koodiArvo})""") ++ lapset.map(lapsi => lapsi._2)))

  def tallennaSuoritukset(versio: VersioEntiteetti, juuriSuoritus: Suoritus): SuoritusEntiteetti =
    val (juuriSuoritusEntiteetti, inserts) = puraSuoritukset(versio, juuriSuoritus, None)
    Await.result(db.run(inserts.transactionally), DB_TIMEOUT)
    juuriSuoritusEntiteetti

  private def haeSuoritukset(versioTunnisteetQuery: slick.jdbc.SQLActionBuilder): Map[VersioEntiteetti, Seq[SuoritusEntiteetti]] =
    var parents: Map[String, Seq[SuoritusEntiteetti]] = Map.empty
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
                path || s.tunniste
              FROM suoritukset s, parents p
              WHERE p.tunniste=s.parent_tunniste
              AND (NOT (s.tunniste = ANY(path))) -- estetään syklit
          )
          SELECT
            suoritukset.tunniste,
            suoritukset.parent_tunniste,
            suoritukset.koodiarvo,
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
        val entiteetti = SuoritusEntiteetti(UUID.fromString(tunniste), koodiArvo, parents.get(tunniste).getOrElse(Seq.empty))
        parents = parents.updatedWith(parentTunniste)(children => children match
          case Some(children) => Some(entiteetti +: children)
          case None => Some(Seq(entiteetti))
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
          ) -> Seq(entiteetti))
          case (Some(_), Some(_)) => throw new RuntimeException()
          case (None, None)       => throw new RuntimeException()
          case (None, Some(_))    => None
      )
      .flatten
      .toMap

  def haeSuoritukset(oppijaNumero: String): Map[VersioEntiteetti, Seq[SuoritusEntiteetti]] =
    haeSuoritukset(sql"""SELECT tunniste FROM versiot WHERE oppijanumero=${oppijaNumero} AND upper(voimassaolo)='infinity'::timestamptz""")

  def haeSuoritus(tunniste: UUID): Option[SuoritusEntiteetti] =
    Await.result(db.run(
        sql"""
          SELECT tunniste, koodiarvo
          FROM suoritukset
          WHERE tunniste=${tunniste.toString}::UUID
       """.as[(String, String)]), DB_TIMEOUT)
      .map((tunniste, koodiArvo) =>
        SuoritusEntiteetti(UUID.fromString(tunniste), koodiArvo, Seq.empty)).headOption
}

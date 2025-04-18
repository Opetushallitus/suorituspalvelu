package fi.oph.suorituspalvelu.business

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.oph.suorituspalvelu.business.KantaOperaatiot.MAPPER
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

object KantaOperaatiot {
  val MAPPER: ObjectMapper = {
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper.registerModule(new JavaTimeModule())
    mapper.registerModule(new Jdk8Module()) // tämä on java.util.Optional -kenttiä varten
    mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, true)
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
    mapper
  }
}

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

  def getAmmatillisenTutkinnonOsaInserts(parentId: UUID, suoritus: AmmatillisenTutkinnonOsa): DBIOAction[_, NoStream, Effect] =
    DBIO.sequence(Seq(sqlu"""INSERT INTO ammatillisen_tutkinnon_osat(tutkinto_tunniste, nimi, koodi, arvosana) VALUES(${parentId.toString}::uuid, ${suoritus.nimi}, ${suoritus.koodi}, ${suoritus.arvosana})"""))

  def getAmmatillinenTutkintoInserts(versio: VersioEntiteetti, suoritus: AmmatillinenTutkinto): DBIOAction[_, NoStream, Effect] =
    val tunniste = getUUID()
    val lapset = suoritus.osat.map(osa => getAmmatillisenTutkinnonOsaInserts(tunniste, osa))
    DBIO.sequence(Seq(sqlu"""INSERT INTO ammatilliset_tutkinnot(tunniste, versio_tunniste, nimi, koodi, vahvistuspaivamaara) VALUES(${tunniste.toString}::uuid, ${versio.tunniste.toString}::uuid, ${suoritus.nimi}, ${suoritus.koodi}, ${suoritus.vahvistusPaivamaara.map(d => d.toString)}::date)""") ++ lapset)

  def getPerusopetuksenOppimaaranAineInserts(parentId: UUID, suoritus: PerusopetuksenOppiaine): DBIOAction[_, NoStream, Effect] =
    DBIO.sequence(Seq(sqlu"""INSERT INTO perusopetuksen_oppiaineet(oppimaara_tunniste, nimi, koodi, arvosana) VALUES(${parentId.toString}::uuid, ${suoritus.nimi}, ${suoritus.koodi}, ${suoritus.arvosana})"""))

  def getPerusopetuksenOppimaaraInserts(versio: VersioEntiteetti, suoritus: PerusopetuksenOppimaara): DBIOAction[_, NoStream, Effect] =
    val tunniste = getUUID()
    val lapset = suoritus.aineet.map(osa => getPerusopetuksenOppimaaranAineInserts(tunniste, osa))
    DBIO.sequence(Seq(sqlu"""INSERT INTO perusopetuksen_oppimaarat(tunniste, versio_tunniste, vahvistuspaivamaara) VALUES(${tunniste.toString}::uuid, ${versio.tunniste.toString}::uuid, ${suoritus.vahvistusPaivamaara.map(d => d.toString)}::date)""") ++ lapset)

  def tallennaSuoritukset(versio: VersioEntiteetti, suoritus: Suoritus): Unit =
    val poistaSuoritukset = poistaVersionSuoritukset(versio) // TODO: tämän pitää perustua on delete cascadeen

    val inserts = suoritus match
      case s: AmmatillinenTutkinto => getAmmatillinenTutkintoInserts(versio, s)
      case s: PerusopetuksenOppimaara => getPerusopetuksenOppimaaraInserts(versio, s)
      case s: GenericSuoritus => throw new RuntimeException() // TODO: tämä ei relevantti

    Await.result(db.run(DBIO.sequence(Seq(poistaSuoritukset, inserts)).transactionally), DB_TIMEOUT)

  private def haeSuorituksetNewInternal(versioTunnisteetQuery: slick.jdbc.SQLActionBuilder): Map[VersioEntiteetti, Seq[Suoritus]] =
    var ammatillisenTutkinnonOsat: Map[String, Seq[AmmatillisenTutkinnonOsa]] = Map.empty
    var perusopetuksenOppiaineet: Map[String, Seq[PerusopetuksenOppiaine]] = Map.empty
    Await.result(db.run(
        (sql"""
          WITH
            versiotunnisteet(tunniste) AS ("""
            concat
            versioTunnisteetQuery
            concat
            sql"""),
            w_versiot AS (
              SELECT versiot.tunniste, jsonb_build_object(
                'tunniste', versiot.tunniste,
                'oppijaNumero', versiot.oppijanumero,
                'alku',to_json(lower(versiot.voimassaolo)::timestamptz)#>>'{}',
                'loppu', CASE WHEN upper(versiot.voimassaolo)='infinity'::timestamptz THEN null ELSE to_json(upper(versiot.voimassaolo)::timestamptz)#>>'{}' END,
                'tietolahde', versiot.lahde
              )::text AS versio
              FROM versiotunnisteet JOIN versiot ON versiotunnisteet.tunniste=versiot.tunniste
            ),
            w_ammatilliset_tutkinnot AS (
              SELECT
                2 AS priority,
                'ammatillinen_tutkinto' AS tyyppi,
                ammatilliset_tutkinnot.tunniste AS tunniste,
                null::uuid AS parent_tunniste,
                jsonb_build_object('nimi', nimi, 'koodi', koodi, 'vahvistusPaivamaara', vahvistuspaivamaara)::text AS data,
                w_versiot.versio AS versio
              FROM ammatilliset_tutkinnot
              INNER JOIN w_versiot ON w_versiot.tunniste=ammatilliset_tutkinnot.versio_tunniste),
            w_ammatillisen_tutkinnon_osat AS (
              SELECT
                1 AS priority,
                'ammatillisen_tutkinnon_osa' AS tyyppi,
                null::uuid AS tunniste,
                tutkinto_tunniste AS parent_tunniste,
                jsonb_build_object('nimi', nimi, 'koodi', koodi, 'arvosana', arvosana)::text AS data,
                null::text AS versio
              FROM ammatillisen_tutkinnon_osat
              JOIN w_ammatilliset_tutkinnot ON tutkinto_tunniste=w_ammatilliset_tutkinnot.tunniste),
            w_perusopetuksen_oppimaarat AS (
              SELECT
                2 AS priority,
                'perusopetuksen_oppimaara' AS tyyppi,
                perusopetuksen_oppimaarat.tunniste AS tunniste,
                null::uuid AS parent_tunniste,
                jsonb_build_object('vahvistusPaivamaara', vahvistuspaivamaara)::text AS data,
                w_versiot.versio AS versio
              FROM perusopetuksen_oppimaarat
              INNER JOIN w_versiot ON w_versiot.tunniste=perusopetuksen_oppimaarat.versio_tunniste),
            w_perusopetuksen_oppiaineet AS (
              SELECT
                1 AS priority,
                'perusopetuksen_oppiaine' AS tyyppi,
                null::uuid AS tunniste,
                oppimaara_tunniste AS parent_tunniste,
                jsonb_build_object('nimi', nimi, 'koodi', koodi, 'arvosana', arvosana)::text AS data,
                null::text AS versio
              FROM perusopetuksen_oppiaineet
              JOIN w_perusopetuksen_oppimaarat ON oppimaara_tunniste=w_perusopetuksen_oppimaarat.tunniste)
          SELECT * FROM w_ammatillisen_tutkinnon_osat
          UNION ALL
          SELECT * FROM w_perusopetuksen_oppiaineet
          UNION ALL
          SELECT * FROM w_ammatilliset_tutkinnot
          UNION ALL
          SELECT * FROM w_perusopetuksen_oppimaarat
          ORDER BY priority ASC;
        """).as[(Int, String, String, String, String, String)]), DB_TIMEOUT)
      .map((_, tyyppi, tunniste, parentTunniste, data, versioData) =>
        tyyppi match
          case "ammatillinen_tutkinto" =>
            val tutkinto = MAPPER.readValue(data, classOf[AmmatillinenTutkinto]).copy(osat = ammatillisenTutkinnonOsat.getOrElse(tunniste, Seq.empty))
            val versio = MAPPER.readValue(versioData, classOf[VersioEntiteetti])
            Some(versio -> tutkinto)
          case "ammatillisen_tutkinnon_osa" =>
            val osa = MAPPER.readValue(data, classOf[AmmatillisenTutkinnonOsa])
            ammatillisenTutkinnonOsat = ammatillisenTutkinnonOsat.updatedWith(parentTunniste)(osat => osat match
              case Some(osat) => Some(osa +: osat)
              case None => Some(Seq(osa))
            )
            None
          case "perusopetuksen_oppimaara" =>
            val tutkinto = MAPPER.readValue(data, classOf[PerusopetuksenOppimaara]).copy(aineet = perusopetuksenOppiaineet.getOrElse(tunniste, Seq.empty))
            val versio = MAPPER.readValue(versioData, classOf[VersioEntiteetti])
            Some(versio -> tutkinto)
          case "perusopetuksen_oppiaine" =>
            val osa = MAPPER.readValue(data, classOf[PerusopetuksenOppiaine])
            perusopetuksenOppiaineet = perusopetuksenOppiaineet.updatedWith(parentTunniste)(osat => osat match
              case Some(osat) => Some(osa +: osat)
              case None => Some(Seq(osa))
            )
            None
          case default => None
      )
      .flatten
      .groupBy((versio, _) => versio)
      .map((versio, suoritukset) => (versio, suoritukset.map(t => t._2)))

  def haeSuorituksetNew(oppijaNumero: String): Map[VersioEntiteetti, Seq[Suoritus]] =
    haeSuorituksetNewInternal(sql"""SELECT tunniste FROM versiot WHERE oppijanumero=${oppijaNumero} AND upper(voimassaolo)='infinity'::timestamptz""")

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

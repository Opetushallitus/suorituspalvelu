package fi.oph.suorituspalvelu.business

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.oph.suorituspalvelu.business.KantaOperaatiot.{MAPPER, SuoritusTyypit}
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
import KantaOperaatiot.SuoritusTyypit.*

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

  enum SuoritusTyypit:
    case AMMATILLINEN_TUTKINTO, AMMATILLISEN_TUTKINNON_OSA, PERUSOPETUKSEN_OPPIMAARA,
    NUORTEN_PERUSOPETUKSEN_OPPIAINEEN_OPPIMAARA, PERUSOPETUKSEN_VUOSILUOKKA, PERUSOPETUKSEN_OPPIAINE, TUVA, TELMA


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
/*
    if(tietolahde==Tietolahde.VIRKAILIJA)
      throw new RuntimeException("Virkailijan versioita ei voi tallentaa tällä metodilla")
*/

    if(sameAsExistingData(oppijaNumero, tietolahde, data)) // TODO: tämä pitää tehdä samassa transaktiossa kun on lukko oppijaan!
      None
    else
      val tunniste = getUUID()
      val timestamp = Instant.ofEpochMilli(Instant.now().toEpochMilli)
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

  def poistaVersionSuoritukset(versio: VersioEntiteetti): DBIOAction[_, NoStream, Effect] =
    DBIO.sequence(Seq(
      sqlu"""
          DELETE FROM perusopetuksen_oppimaarat USING versiot WHERE versiot.tunniste=perusopetuksen_oppimaarat.versio_tunniste AND versiot.tunniste=${versio.tunniste.toString}::uuid;
       """,
      sqlu"""
          DELETE FROM ammatilliset_tutkinnot USING versiot WHERE versiot.tunniste=ammatilliset_tutkinnot.versio_tunniste AND versiot.tunniste=${versio.tunniste.toString}::uuid;
       """,
      sqlu"""
          DELETE FROM nuorten_perusopetuksen_oppiaineen_oppimaarat USING versiot WHERE versiot.tunniste=nuorten_perusopetuksen_oppiaineen_oppimaarat.versio_tunniste AND versiot.tunniste=${versio.tunniste.toString}::uuid;
       """,
      sqlu"""
          DELETE FROM perusopetuksen_vuosiluokat USING versiot WHERE versiot.tunniste=perusopetuksen_vuosiluokat.versio_tunniste AND versiot.tunniste=${versio.tunniste.toString}::uuid;
       """,
      sqlu"""
          DELETE FROM tuvat USING versiot WHERE versiot.tunniste=tuvat.versio_tunniste AND versiot.tunniste=${versio.tunniste.toString}::uuid;
       """
    ))

  def getAmmatillisenTutkinnonOsaInserts(parentId: UUID, suoritus: AmmatillisenTutkinnonOsa): DBIOAction[_, NoStream, Effect] =
    DBIO.sequence(Seq(
      sqlu"""INSERT INTO ammatillisen_tutkinnon_osat(tutkinto_tunniste, nimi, koodi, arvosana)
            VALUES(${parentId.toString}::uuid, ${suoritus.nimi}, ${suoritus.koodi}, ${suoritus.arvosana})"""))

  def getAmmatillinenTutkintoInserts(versio: VersioEntiteetti, suoritus: AmmatillinenTutkinto): DBIOAction[_, NoStream, Effect] =
    val tunniste = getUUID()
    val lapset = suoritus.osat.map(osa => getAmmatillisenTutkinnonOsaInserts(tunniste, osa))
    DBIO.sequence(Seq(
      sqlu"""INSERT INTO ammatilliset_tutkinnot(tunniste, versio_tunniste, nimi, koodi, vahvistuspaivamaara)
            VALUES(${tunniste.toString}::uuid, ${versio.tunniste.toString}::uuid, ${suoritus.nimi}, ${suoritus.koodi}, ${suoritus.vahvistusPaivamaara.map(d => d.toString)}::date)""") ++ lapset)

  def getPerusopetuksenVuosiluokkaInserts(versio: VersioEntiteetti, suoritus: PerusopetuksenVuosiluokka): DBIOAction[_, NoStream, Effect] =
    DBIO.sequence(Seq(
      sqlu"""INSERT INTO perusopetuksen_vuosiluokat(versio_tunniste, nimi, koodi)
            VALUES(${versio.tunniste.toString}::uuid, ${suoritus.nimi}, ${suoritus.koodi})"""))

  def getNuortenPerusopetuksenOppiaineenOppimaaraInserts(versio: VersioEntiteetti, suoritus: NuortenPerusopetuksenOppiaineenOppimaara): DBIOAction[_, NoStream, Effect] =
    DBIO.sequence(Seq(
      sqlu"""INSERT INTO nuorten_perusopetuksen_oppiaineen_oppimaarat(versio_tunniste, nimi, koodi, arvosana)
            VALUES(${versio.tunniste.toString}::uuid, ${suoritus.nimi}, ${suoritus.koodi}, ${suoritus.arvosana})"""))

  def getPerusopetuksenOppimaaranAineInserts(parentId: UUID, suoritus: PerusopetuksenOppiaine): DBIOAction[_, NoStream, Effect] =
    DBIO.sequence(Seq(
      sqlu"""INSERT INTO perusopetuksen_oppiaineet(oppimaara_tunniste, nimi, koodi, arvosana)
            VALUES(${parentId.toString}::uuid, ${suoritus.nimi}, ${suoritus.koodi}, ${suoritus.arvosana})"""))

  def getPerusopetuksenOppimaaraInserts(versio: VersioEntiteetti, suoritus: PerusopetuksenOppimaara): DBIOAction[_, NoStream, Effect] =
    val tunniste = getUUID()
    val lapset = suoritus.aineet.map(osa => getPerusopetuksenOppimaaranAineInserts(tunniste, osa))
    DBIO.sequence(Seq(
      sqlu"""INSERT INTO perusopetuksen_oppimaarat(tunniste, versio_tunniste, vahvistuspaivamaara)
            VALUES(${tunniste.toString}::uuid, ${versio.tunniste.toString}::uuid, ${suoritus.vahvistusPaivamaara.map(d => d.toString)}::date)""") ++ lapset)

  def getTuvaInserts(versio: VersioEntiteetti, suoritus: Tuva): DBIOAction[_, NoStream, Effect] =
    DBIO.sequence(Seq(
      sqlu"""INSERT INTO tuvat(versio_tunniste, koodi, vahvistuspaivamaara)
            VALUES(${versio.tunniste.toString}::uuid, ${suoritus.koodi}, ${suoritus.vahvistusPaivamaara.map(d => d.toString)}::date)"""))

  def getTelmaInserts(versio: VersioEntiteetti, suoritus: Telma): DBIOAction[_, NoStream, Effect] =
    DBIO.sequence(Seq(
      sqlu"""INSERT INTO telmat(versio_tunniste, koodi)
            VALUES(${versio.tunniste.toString}::uuid, ${suoritus.koodi})"""))

  def tallennaSuoritukset(versio: VersioEntiteetti, suoritukset: Set[Suoritus]): Unit =
    val poistaSuoritukset = poistaVersionSuoritukset(versio)

    val inserts = suoritukset.map(suoritus => {
      suoritus match
        case s: AmmatillinenTutkinto => getAmmatillinenTutkintoInserts(versio, s)
        case s: PerusopetuksenOppimaara => getPerusopetuksenOppimaaraInserts(versio, s)
        case s: NuortenPerusopetuksenOppiaineenOppimaara => getNuortenPerusopetuksenOppiaineenOppimaaraInserts(versio, s)
        case s: PerusopetuksenVuosiluokka => getPerusopetuksenVuosiluokkaInserts(versio, s)
        case s: Tuva => getTuvaInserts(versio, s)
        case s: Telma => getTelmaInserts(versio, s)
    })

    Await.result(db.run(DBIO.sequence(Seq(poistaSuoritukset, DBIO.sequence(inserts))).transactionally), DB_TIMEOUT)

  private def haeSuorituksetInternal(versioTunnisteetQuery: slick.jdbc.SQLActionBuilder): Map[VersioEntiteetti, Set[Suoritus]] =
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
                ${AMMATILLINEN_TUTKINTO.toString} AS tyyppi,
                ammatilliset_tutkinnot.tunniste AS tunniste,
                null::uuid AS parent_tunniste,
                jsonb_build_object('nimi', nimi, 'koodi', koodi, 'vahvistusPaivamaara', vahvistuspaivamaara)::text AS data,
                w_versiot.versio AS versio
              FROM ammatilliset_tutkinnot
              INNER JOIN w_versiot ON w_versiot.tunniste=ammatilliset_tutkinnot.versio_tunniste),
            w_ammatillisen_tutkinnon_osat AS (
              SELECT
                1 AS priority,
                ${AMMATILLISEN_TUTKINNON_OSA.toString} AS tyyppi,
                null::uuid AS tunniste,
                tutkinto_tunniste AS parent_tunniste,
                jsonb_build_object('nimi', nimi, 'koodi', koodi, 'arvosana', arvosana)::text AS data,
                null::text AS versio
              FROM ammatillisen_tutkinnon_osat
              INNER JOIN w_ammatilliset_tutkinnot ON tutkinto_tunniste=w_ammatilliset_tutkinnot.tunniste),
            w_perusopetuksen_oppimaarat AS (
              SELECT
                2 AS priority,
                ${PERUSOPETUKSEN_OPPIMAARA.toString} AS tyyppi,
                perusopetuksen_oppimaarat.tunniste AS tunniste,
                null::uuid AS parent_tunniste,
                jsonb_build_object('vahvistusPaivamaara', vahvistuspaivamaara)::text AS data,
                w_versiot.versio AS versio
              FROM perusopetuksen_oppimaarat
              INNER JOIN w_versiot ON w_versiot.tunniste=perusopetuksen_oppimaarat.versio_tunniste),
            w_nuorten_perusopetuksen_oppiaineen_oppimaarat AS (
              SELECT
                1 AS priority,
                ${NUORTEN_PERUSOPETUKSEN_OPPIAINEEN_OPPIMAARA.toString} AS tyyppi,
                null::uuid  AS tunniste,
                null::uuid AS parent_tunniste,
                jsonb_build_object('nimi', nimi, 'koodi', koodi, 'arvosana', arvosana)::text AS data,
                w_versiot.versio AS versio
              FROM nuorten_perusopetuksen_oppiaineen_oppimaarat
              INNER JOIN w_versiot ON w_versiot.tunniste=nuorten_perusopetuksen_oppiaineen_oppimaarat.versio_tunniste),
            w_perusopetuksen_vuosiluokat AS (
              SELECT
                1 AS priority,
                ${PERUSOPETUKSEN_VUOSILUOKKA.toString} AS tyyppi,
                null::uuid  AS tunniste,
                null::uuid AS parent_tunniste,
                jsonb_build_object('nimi', nimi, 'koodi', koodi)::text AS data,
                w_versiot.versio AS versio
              FROM perusopetuksen_vuosiluokat
              INNER JOIN w_versiot ON w_versiot.tunniste=perusopetuksen_vuosiluokat.versio_tunniste),
            w_perusopetuksen_oppiaineet AS (
              SELECT
                1 AS priority,
                ${PERUSOPETUKSEN_OPPIAINE.toString} AS tyyppi,
                null::uuid AS tunniste,
                oppimaara_tunniste AS parent_tunniste,
                jsonb_build_object('nimi', nimi, 'koodi', koodi, 'arvosana', arvosana)::text AS data,
                null::text AS versio
              FROM perusopetuksen_oppiaineet
              JOIN w_perusopetuksen_oppimaarat ON oppimaara_tunniste=w_perusopetuksen_oppimaarat.tunniste),
            w_tuvat AS (
              SELECT
                1 AS priority,
                ${TUVA.toString} AS tyyppi,
                null::uuid AS tunniste,
                null::uuid AS parent_tunniste,
                jsonb_build_object('koodi', koodi, 'vahvistusPaivamaara', vahvistuspaivamaara)::text AS data,
                w_versiot.versio AS versio
              FROM tuvat
              INNER JOIN w_versiot ON w_versiot.tunniste=tuvat.versio_tunniste),
            w_telmat AS (
              SELECT
                1 AS priority,
                ${TELMA.toString} AS tyyppi,
                null::uuid AS tunniste,
                null::uuid AS parent_tunniste,
                jsonb_build_object('koodi', koodi)::text AS data,
                w_versiot.versio AS versio
              FROM telmat
              INNER JOIN w_versiot ON w_versiot.tunniste=telmat.versio_tunniste)
          SELECT * FROM w_ammatillisen_tutkinnon_osat
          UNION ALL
          SELECT * FROM w_perusopetuksen_oppiaineet
          UNION ALL
          SELECT * FROM w_ammatilliset_tutkinnot
          UNION ALL
          SELECT * FROM w_perusopetuksen_oppimaarat
          UNION ALL
          SELECT * FROM w_nuorten_perusopetuksen_oppiaineen_oppimaarat
          UNION ALL
          SELECT * FROM w_perusopetuksen_vuosiluokat
          UNION ALL
          SELECT * FROM w_tuvat
          UNION ALL
          SELECT * FROM w_telmat
          ORDER BY priority ASC;
        """).as[(Int, String, String, String, String, String)]), DB_TIMEOUT)
      .map((_, tyyppi, tunniste, parentTunniste, data, versioData) =>
        val versio = Option.apply(versioData).map(data => MAPPER.readValue(data, classOf[VersioEntiteetti]))
        SuoritusTyypit.valueOf(tyyppi) match
          case AMMATILLINEN_TUTKINTO =>
            val tutkinto = MAPPER.readValue(data, classOf[AmmatillinenTutkinto]).copy(osat = ammatillisenTutkinnonOsat.getOrElse(tunniste, Seq.empty).toSet)
            Some(versio.get -> tutkinto)
          case AMMATILLISEN_TUTKINNON_OSA =>
            val osa = MAPPER.readValue(data, classOf[AmmatillisenTutkinnonOsa])
            ammatillisenTutkinnonOsat = ammatillisenTutkinnonOsat.updatedWith(parentTunniste)(osat => osat match
              case Some(osat) => Some(osa +: osat)
              case None => Some(Seq(osa))
            )
            None
          case PERUSOPETUKSEN_OPPIMAARA =>
            val tutkinto = MAPPER.readValue(data, classOf[PerusopetuksenOppimaara]).copy(aineet = perusopetuksenOppiaineet.getOrElse(tunniste, Seq.empty).toSet)
            Some(versio.get -> tutkinto)
          case NUORTEN_PERUSOPETUKSEN_OPPIAINEEN_OPPIMAARA =>
            val tutkinto = MAPPER.readValue(data, classOf[NuortenPerusopetuksenOppiaineenOppimaara])
            Some(versio.get -> tutkinto)
          case PERUSOPETUKSEN_VUOSILUOKKA =>
            val tutkinto = MAPPER.readValue(data, classOf[PerusopetuksenVuosiluokka])
            Some(versio.get -> tutkinto)
          case TUVA =>
            val tutkinto = MAPPER.readValue(data, classOf[Tuva])
            Some(versio.get -> tutkinto)
          case TELMA =>
            val tutkinto = MAPPER.readValue(data, classOf[Telma])
            Some(versio.get -> tutkinto)
          case PERUSOPETUKSEN_OPPIAINE =>
            val osa = MAPPER.readValue(data, classOf[PerusopetuksenOppiaine])
            perusopetuksenOppiaineet = perusopetuksenOppiaineet.updatedWith(parentTunniste)(osat => osat match
              case Some(osat) => Some(osa +: osat)
              case None => Some(Seq(osa))
            )
            None
          case default =>
            None
      )
      .flatten
      .groupBy((versio, _) => versio)
      .map((versio, suoritukset) => (versio, suoritukset.map(t => t._2).toSet))

  def haeSuoritukset(oppijaNumero: String): Map[VersioEntiteetti, Set[Suoritus]] =
    haeSuorituksetInternal(sql"""SELECT tunniste FROM versiot WHERE oppijanumero=${oppijaNumero} AND upper(voimassaolo)='infinity'::timestamptz""")
}

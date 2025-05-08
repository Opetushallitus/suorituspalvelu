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

import scala.concurrent.ExecutionContext.Implicits.global
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
    case AMMATILLINEN_TUTKINTO, AMMATILLISEN_TUTKINNON_OSA, AMMATILLISEN_TUTKINNON_OSAALUE, PERUSOPETUKSEN_OPPIMAARA,
    NUORTEN_PERUSOPETUKSEN_OPPIAINEEN_OPPIMAARA, PERUSOPETUKSEN_VUOSILUOKKA, PERUSOPETUKSEN_OPPIAINE, TUVA, TELMA
}

class KantaOperaatiot(db: JdbcBackend.JdbcDatabaseDef) {

  final val DB_TIMEOUT = 30.seconds
  val LOG = LoggerFactory.getLogger(classOf[KantaOperaatiot])

  def getUUID(): UUID =
    UUID.randomUUID()

  def sameAsExistingData(oppijaNumero: String, tietolahde: Tietolahde, data: String): DBIOAction[Boolean, NoStream, Effect] =
    tietolahde match
      case Tietolahde.KOSKI | Tietolahde.YTR =>
        sql"""
            SELECT data_json
            FROM versiot
            WHERE oppijanumero=${oppijaNumero} AND lahde=${tietolahde.toString}::lahde AND upper(voimassaolo)='infinity'::timestamptz
        """.as[String].map(existingData => {
            if(existingData.isEmpty)
              false
            else
              JSONCompare.compareJSON(existingData.head, data, JSONCompareMode.NON_EXTENSIBLE).passed()
          })
      case default => sql"""SELECT 1""".as[String].map(_ => false) // ei toteutettu

  def tallennaJarjestelmaVersio(oppijaNumero: String, tietolahde: Tietolahde, data: String): Option[VersioEntiteetti] =
    val insertOppijaAction = sqlu"INSERT INTO oppijat(oppijanumero) VALUES (${oppijaNumero}) ON CONFLICT DO NOTHING"
    val lockOppijaAction = sql"""SELECT 1 FROM oppijat WHERE oppijanumero=${oppijaNumero} FOR UPDATE"""
    val insertVersioIfNewDataAction = DBIO.sequence(Seq(insertOppijaAction, lockOppijaAction.as[Int]))
      .flatMap(_ => sameAsExistingData(oppijaNumero, tietolahde, data)).flatMap(isSame => {
        if (isSame)
          DBIO.sequence(Seq.empty).map(_ => None)
        else
          val tunniste = getUUID()
          val timestamp = Instant.ofEpochMilli(Instant.now().toEpochMilli)
          val discontinueOldVersionAction =
            sql"""
                 UPDATE versiot
                 SET voimassaolo=tstzrange(lower(voimassaolo), ${timestamp.toString}::timestamptz)
                 WHERE oppijanumero=${oppijaNumero} AND lahde=${tietolahde.toString}::lahde AND upper(voimassaolo)='infinity'::timestamptz
                 RETURNING tunniste::text""".as[String]
          val insertVersioAction = discontinueOldVersionAction
            .flatMap(useVersioTunnisteet => {
              val useVersioTunniste = if (useVersioTunnisteet.isEmpty) null else useVersioTunnisteet.head
              tietolahde match
                case VIRTA => sqlu"""
                    INSERT INTO versiot(tunniste, use_versio_tunniste, oppijanumero, voimassaolo, lahde, data_json)
                    VALUES(${tunniste.toString}::uuid, ${useVersioTunniste}::uuid, ${oppijaNumero}, tstzrange(${timestamp.toString}::timestamptz, 'infinity'::timestamptz), ${tietolahde.toString}::lahde, ${data}::xml)"""
                case default => sqlu"""
                    INSERT INTO versiot(tunniste, use_versio_tunniste, oppijanumero, voimassaolo, lahde, data_json)
                    VALUES(${tunniste.toString}::uuid, ${useVersioTunniste}::uuid, ${oppijaNumero}, tstzrange(${timestamp.toString}::timestamptz, 'infinity'::timestamptz), ${tietolahde.toString}::lahde, ${data}::jsonb)"""
            })
          insertVersioAction.map(_ => Some(VersioEntiteetti(tunniste, oppijaNumero, timestamp, None, tietolahde)))
      })
    Await.result(db.run(insertVersioIfNewDataAction.transactionally), DB_TIMEOUT)

  def haeData(versio: VersioEntiteetti): (VersioEntiteetti, String) =
    Await.result(db.run(
      sql"""SELECT jsonb_build_object('tunniste', tunniste,
              'oppijaNumero', oppijanumero,
              'alku',to_json(lower(voimassaolo)::timestamptz)#>>'{}',
              'loppu', CASE WHEN upper(voimassaolo)='infinity'::timestamptz THEN null ELSE to_json(upper(voimassaolo)::timestamptz)#>>'{}' END,
              'tietolahde', lahde
            )::text AS versio,
            CASE WHEN lahde='VIRTA'::lahde THEN data_xml::text ELSE data_json::text END
            FROM versiot
            WHERE tunniste=${versio.tunniste.toString}::UUID""".as[(String, String)]), DB_TIMEOUT)
      .map((json, data) => (MAPPER.readValue(json, classOf[VersioEntiteetti]), data)).head

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
       """,
      sqlu"""
          DELETE FROM telmat USING versiot WHERE versiot.tunniste=telmat.versio_tunniste AND versiot.tunniste=${versio.tunniste.toString}::uuid;
       """
    ))

  def getAmmatillisenTutkinnonOsaAlueInserts(parentId: Int, suoritus: AmmatillisenTutkinnonOsaAlue): DBIOAction[_, NoStream, Effect] =
    DBIO.sequence(Seq(
      sqlu"""INSERT INTO ammatillisen_tutkinnon_osaalueet(osa_tunniste, nimi, koodi, koodisto, koodistoversio, arvosana, arvosanaasteikko, arvosanaversio, laajuus, laajuuskoodi, laajuuskoodisto, laajuusversio)
            VALUES(${parentId}, ${suoritus.nimi}, ${suoritus.koodi.arvo}, ${suoritus.koodi.koodisto}, ${suoritus.koodi.versio}, ${suoritus.arvosana.map(a => a.arvo)}, ${suoritus.arvosana.map(a => a.koodisto)}, ${suoritus.arvosana.map(a => a.versio)}, ${suoritus.laajuus}, ${suoritus.laajuusKoodi.arvo}, ${suoritus.laajuusKoodi.koodisto}, ${suoritus.laajuusKoodi.versio})"""))

  def getAmmatillisenTutkinnonOsaInserts(parentId: Int, suoritus: AmmatillisenTutkinnonOsa): DBIOAction[_, NoStream, Effect] =
    sql"""INSERT INTO ammatillisen_tutkinnon_osat(tutkinto_tunniste, nimi, koodi, koodisto, koodistoversio, yto, arvosana, arvosanaasteikko, arvosanaversio, laajuus, laajuuskoodi, laajuuskoodisto, laajuusversio)
            VALUES(${parentId}, ${suoritus.nimi}, ${suoritus.koodi.arvo}, ${suoritus.koodi.koodisto}, ${suoritus.koodi.versio}, ${suoritus.yto}, ${suoritus.arvosana.map(a => a.arvo)}, ${suoritus.arvosana.map(a => a.koodisto)}, ${suoritus.arvosana.map(a => a.versio)}, ${suoritus.laajuus}, ${suoritus.laajuusKoodi.arvo}, ${suoritus.laajuusKoodi.koodisto}, ${suoritus.laajuusKoodi.versio}) RETURNING tunniste""".as[(Int)].flatMap(osaTunnisteet => {
      DBIO.sequence(osaTunnisteet.map(tunniste => suoritus.osaAlueet.map(osa => getAmmatillisenTutkinnonOsaAlueInserts(tunniste, osa))).flatten)
    })

  def getAmmatillinenTutkintoInserts(versio: VersioEntiteetti, suoritus: AmmatillinenTutkinto): DBIOAction[_, NoStream, Effect] =
    sql"""INSERT INTO ammatilliset_tutkinnot(versio_tunniste, nimi, tyyppi, koodisto, koodistoversio, tila, tilakoodisto, tilaversio, suoritustapa, suoritustapakoodisto, suoritustapaversio, keskiarvo, vahvistuspaivamaara)
            VALUES(${versio.tunniste.toString}::uuid, ${suoritus.nimi}, ${suoritus.tyyppi.arvo}, ${suoritus.tyyppi.koodisto}, ${suoritus.tyyppi.versio}, ${suoritus.tila.arvo}, ${suoritus.tila.koodisto}, ${suoritus.tila.versio}, ${suoritus.suoritustapa.arvo}, ${suoritus.suoritustapa.koodisto}, ${suoritus.suoritustapa.versio}, ${suoritus.keskiarvo}, ${suoritus.vahvistusPaivamaara.map(d => d.toString)}::date) RETURNING tunniste""".as[(Int)].flatMap(tutkintoTunnisteet => {
      DBIO.sequence(tutkintoTunnisteet.map(tunniste => suoritus.osat.map(osa => getAmmatillisenTutkinnonOsaInserts(tunniste, osa))).flatten)
    })

  def getPerusopetuksenVuosiluokkaInserts(versio: VersioEntiteetti, suoritus: PerusopetuksenVuosiluokka): DBIOAction[_, NoStream, Effect] =
    DBIO.sequence(Seq(
      sqlu"""INSERT INTO perusopetuksen_vuosiluokat(versio_tunniste, nimi, koodi)
            VALUES(${versio.tunniste.toString}::uuid, ${suoritus.nimi}, ${suoritus.koodi})"""))

  def getNuortenPerusopetuksenOppiaineenOppimaaraInserts(versio: VersioEntiteetti, suoritus: NuortenPerusopetuksenOppiaineenOppimaara): DBIOAction[_, NoStream, Effect] =
    DBIO.sequence(Seq(
      sqlu"""INSERT INTO nuorten_perusopetuksen_oppiaineen_oppimaarat(versio_tunniste, nimi, koodi, arvosana)
            VALUES(${versio.tunniste.toString}::uuid, ${suoritus.nimi}, ${suoritus.koodi}, ${suoritus.arvosana})"""))

  def getPerusopetuksenOppimaaranAineInserts(parentId: Int, suoritus: PerusopetuksenOppiaine): DBIOAction[_, NoStream, Effect] =
    DBIO.sequence(Seq(
      sqlu"""INSERT INTO perusopetuksen_oppiaineet(oppimaara_tunniste, nimi, koodi, arvosana)
            VALUES(${parentId}, ${suoritus.nimi}, ${suoritus.koodi}, ${suoritus.arvosana})"""))

  def getPerusopetuksenOppimaaraInserts(versio: VersioEntiteetti, suoritus: PerusopetuksenOppimaara): DBIOAction[_, NoStream, Effect] =
    sql"""INSERT INTO perusopetuksen_oppimaarat(versio_tunniste, vahvistuspaivamaara)
            VALUES(${versio.tunniste.toString}::uuid, ${suoritus.vahvistusPaivamaara.map(d => d.toString)}::date) RETURNING tunniste""".as[(Int)].flatMap(oppimaaraTunnisteet => {
      DBIO.sequence(oppimaaraTunnisteet.map(tunniste => suoritus.aineet.map(osa => getPerusopetuksenOppimaaranAineInserts(tunniste, osa))).flatten)
    })

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
    val otaVersioKayttoon = sqlu"""UPDATE versiot SET use_versio_tunniste=NULL WHERE tunniste=${versio.tunniste.toString}::uuid"""

    val inserts = suoritukset.map(suoritus => {
      suoritus match
        case s: AmmatillinenTutkinto => getAmmatillinenTutkintoInserts(versio, s)
        case s: PerusopetuksenOppimaara => getPerusopetuksenOppimaaraInserts(versio, s)
        case s: NuortenPerusopetuksenOppiaineenOppimaara => getNuortenPerusopetuksenOppiaineenOppimaaraInserts(versio, s)
        case s: PerusopetuksenVuosiluokka => getPerusopetuksenVuosiluokkaInserts(versio, s)
        case s: Tuva => getTuvaInserts(versio, s)
        case s: Telma => getTelmaInserts(versio, s)
    })

    Await.result(db.run(DBIO.sequence(Seq(otaVersioKayttoon, poistaSuoritukset, DBIO.sequence(inserts))).transactionally), DB_TIMEOUT)

  private def haeSuorituksetInternal(versioTunnisteetQuery: slick.jdbc.SQLActionBuilder): Map[VersioEntiteetti, Set[Suoritus]] =
    var ammatillisenTutkinnonOsaAlueet: Map[String, Seq[AmmatillisenTutkinnonOsaAlue]] = Map.empty
    var ammatillisenTutkinnonOsat: Map[String, Seq[AmmatillisenTutkinnonOsa]] = Map.empty
    var perusopetuksenOppiaineet: Map[String, Seq[PerusopetuksenOppiaine]] = Map.empty
    Await.result(db.run(
        (sql"""
          WITH RECURSIVE
            w_versiotunnisteet(tunniste) AS ("""
            concat
            versioTunnisteetQuery
            concat
            sql"""),
            w_versiot_in_use(tunniste, use_versio_tunniste, loppu) AS (
                SELECT versiot.tunniste, versiot.use_versio_tunniste, upper(versiot.voimassaolo)
                FROM w_versiotunnisteet JOIN versiot ON w_versiotunnisteet.tunniste=versiot.tunniste
              UNION ALL
                SELECT versiot.tunniste, versiot.use_versio_tunniste, w_versiot_in_use.loppu
                FROM w_versiot_in_use JOIN versiot ON w_versiot_in_use.use_versio_tunniste=versiot.tunniste
                WHERE w_versiot_in_use.loppu>upper(versiot.voimassaolo) -- estetään syklit
            ),
            w_versiot AS (
              SELECT versiot.tunniste, jsonb_build_object(
                'tunniste', versiot.tunniste,
                'oppijaNumero', versiot.oppijanumero,
                'alku',to_json(lower(versiot.voimassaolo)::timestamptz)#>>'{}',
                'loppu', CASE WHEN loppu='infinity'::timestamptz THEN null ELSE to_json(loppu::timestamptz)#>>'{}' END,
                'tietolahde', versiot.lahde
              )::text AS versio
              FROM w_versiot_in_use JOIN versiot ON w_versiot_in_use.tunniste=versiot.tunniste
              WHERE w_versiot_in_use.use_versio_tunniste IS NULL
            ),
            w_ammatilliset_tutkinnot AS (
              SELECT
                1 AS priority,
                ${AMMATILLINEN_TUTKINTO.toString} AS tyyppi,
                ammatilliset_tutkinnot.tunniste AS tunniste,
                null::int AS parent_tunniste,
                jsonb_build_object(
                  'nimi', nimi,
                  'tyyppi', jsonb_build_object('arvo', tyyppi, 'koodisto', koodisto, 'versio', koodistoversio),
                  'tila', jsonb_build_object('arvo', tila, 'koodisto', tilakoodisto, 'versio', tilaversio),
                  'vahvistusPaivamaara', vahvistuspaivamaara,
                  'keskiarvo', keskiarvo,
                  'suoritustapa', jsonb_build_object('arvo', suoritustapa, 'koodisto', suoritustapakoodisto, 'versio', suoritustapaversio)
                )::text AS data,
                w_versiot.versio AS versio
              FROM ammatilliset_tutkinnot
              INNER JOIN w_versiot ON w_versiot.tunniste=ammatilliset_tutkinnot.versio_tunniste),
            w_ammatillisen_tutkinnon_osat AS (
              SELECT
                2 AS priority,
                ${AMMATILLISEN_TUTKINNON_OSA.toString} AS tyyppi,
                ammatillisen_tutkinnon_osat.tunniste AS tunniste,
                tutkinto_tunniste AS parent_tunniste,
                jsonb_build_object(
                  'nimi', nimi,
                  'koodi', jsonb_build_object('arvo', koodi, 'koodisto', koodisto, 'versio', koodistoversio),
                  'yto', yto,
                  'arvosana', CASE WHEN arvosana IS NOT NULL THEN jsonb_build_object('arvo', arvosana, 'koodisto', arvosanaasteikko, 'versio', arvosanaversio) ELSE null END,
                  'laajuus', laajuus,
                  'laajuusKoodi', jsonb_build_object('arvo', laajuuskoodi, 'koodisto', laajuuskoodisto, 'versio', laajuusversio)
                )::text AS data,
                null::text AS versio
              FROM ammatillisen_tutkinnon_osat
              INNER JOIN w_ammatilliset_tutkinnot ON tutkinto_tunniste=w_ammatilliset_tutkinnot.tunniste),
            w_ammatillisen_tutkinnon_osaalueet AS (
              SELECT
                3 AS priority,
                ${AMMATILLISEN_TUTKINNON_OSAALUE.toString} AS tyyppi,
                null::int AS tunniste,
                osa_tunniste AS parent_tunniste,
                jsonb_build_object(
                  'nimi', nimi,
                  'koodi', jsonb_build_object('arvo', koodi, 'koodisto', koodisto, 'versio', koodistoversio),
                  'arvosana', CASE WHEN arvosana IS NOT NULL THEN jsonb_build_object('arvo', arvosana, 'koodisto', arvosanaasteikko, 'versio', arvosanaversio) ELSE null END,
                  'laajuus', laajuus,
                  'laajuusKoodi', jsonb_build_object('arvo', laajuuskoodi, 'koodisto', laajuuskoodisto, 'versio', laajuusversio)
                )::text AS data,
                null::text AS versio
              FROM ammatillisen_tutkinnon_osaalueet
              INNER JOIN w_ammatillisen_tutkinnon_osat ON osa_tunniste=w_ammatillisen_tutkinnon_osat.tunniste),
            w_perusopetuksen_oppimaarat AS (
              SELECT
                4 AS priority,
                ${PERUSOPETUKSEN_OPPIMAARA.toString} AS tyyppi,
                perusopetuksen_oppimaarat.tunniste AS tunniste,
                null::int AS parent_tunniste,
                jsonb_build_object('vahvistusPaivamaara', vahvistuspaivamaara)::text AS data,
                w_versiot.versio AS versio
              FROM perusopetuksen_oppimaarat
              INNER JOIN w_versiot ON w_versiot.tunniste=perusopetuksen_oppimaarat.versio_tunniste),
            w_nuorten_perusopetuksen_oppiaineen_oppimaarat AS (
              SELECT
                5 AS priority,
                ${NUORTEN_PERUSOPETUKSEN_OPPIAINEEN_OPPIMAARA.toString} AS tyyppi,
                null::int AS tunniste,
                null::int AS parent_tunniste,
                jsonb_build_object('nimi', nimi, 'koodi', koodi, 'arvosana', arvosana)::text AS data,
                w_versiot.versio AS versio
              FROM nuorten_perusopetuksen_oppiaineen_oppimaarat
              INNER JOIN w_versiot ON w_versiot.tunniste=nuorten_perusopetuksen_oppiaineen_oppimaarat.versio_tunniste),
            w_perusopetuksen_vuosiluokat AS (
              SELECT
                6 AS priority,
                ${PERUSOPETUKSEN_VUOSILUOKKA.toString} AS tyyppi,
                null::int AS tunniste,
                null::int AS parent_tunniste,
                jsonb_build_object('nimi', nimi, 'koodi', koodi)::text AS data,
                w_versiot.versio AS versio
              FROM perusopetuksen_vuosiluokat
              INNER JOIN w_versiot ON w_versiot.tunniste=perusopetuksen_vuosiluokat.versio_tunniste),
            w_perusopetuksen_oppiaineet AS (
              SELECT
                7 AS priority,
                ${PERUSOPETUKSEN_OPPIAINE.toString} AS tyyppi,
                null::int AS tunniste,
                oppimaara_tunniste AS parent_tunniste,
                jsonb_build_object('nimi', nimi, 'koodi', koodi, 'arvosana', arvosana)::text AS data,
                null::text AS versio
              FROM perusopetuksen_oppiaineet
              JOIN w_perusopetuksen_oppimaarat ON oppimaara_tunniste=w_perusopetuksen_oppimaarat.tunniste),
            w_tuvat AS (
              SELECT
                8 AS priority,
                ${TUVA.toString} AS tyyppi,
                null::int AS tunniste,
                null::int AS parent_tunniste,
                jsonb_build_object('koodi', koodi, 'vahvistusPaivamaara', vahvistuspaivamaara)::text AS data,
                w_versiot.versio AS versio
              FROM tuvat
              INNER JOIN w_versiot ON w_versiot.tunniste=tuvat.versio_tunniste),
            w_telmat AS (
              SELECT
                9 AS priority,
                ${TELMA.toString} AS tyyppi,
                null::int AS tunniste,
                null::int AS parent_tunniste,
                jsonb_build_object('koodi', koodi)::text AS data,
                w_versiot.versio AS versio
              FROM telmat
              INNER JOIN w_versiot ON w_versiot.tunniste=telmat.versio_tunniste)
          SELECT * FROM w_ammatillisen_tutkinnon_osaalueet
          UNION ALL
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
          ORDER BY priority DESC;
        """).as[(Int, String, String, String, String, String)]), DB_TIMEOUT)
      .map((_, tyyppi, tunniste, parentTunniste, data, versioData) =>
        val versio = Option.apply(versioData).map(data => MAPPER.readValue(data, classOf[VersioEntiteetti]))
        SuoritusTyypit.valueOf(tyyppi) match
          case AMMATILLINEN_TUTKINTO =>
            val tutkinto = MAPPER.readValue(data, classOf[AmmatillinenTutkinto]).copy(osat = ammatillisenTutkinnonOsat.getOrElse(tunniste, Seq.empty).toSet)
            Some(versio.get -> tutkinto)
          case AMMATILLISEN_TUTKINNON_OSAALUE =>
            val osa = MAPPER.readValue(data, classOf[AmmatillisenTutkinnonOsaAlue])
            ammatillisenTutkinnonOsaAlueet = ammatillisenTutkinnonOsaAlueet.updatedWith(parentTunniste)(osat => osat match
              case Some(osat) => Some(osa +: osat)
              case None => Some(Seq(osa))
            )
            None
          case AMMATILLISEN_TUTKINNON_OSA =>
            val osa = MAPPER.readValue(data, classOf[AmmatillisenTutkinnonOsa]).copy(osaAlueet = ammatillisenTutkinnonOsaAlueet.getOrElse(tunniste, Seq.empty).toSet)
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

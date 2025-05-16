package fi.oph.suorituspalvelu.business

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.oph.suorituspalvelu.business.KantaOperaatiot.{MAPPER, KantaEntiteetit}
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
import KantaOperaatiot.KantaEntiteetit.*
import fi.oph.suorituspalvelu.parsing.koski.KoskiLisatiedot

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

  enum KantaEntiteetit:
    case AMMATILLINEN_TUTKINTO, AMMATILLISEN_TUTKINNON_OSA, AMMATILLISEN_TUTKINNON_OSAALUE, PERUSOPETUKSEN_OPPIMAARA,
    NUORTEN_PERUSOPETUKSEN_OPPIAINEEN_OPPIMAARA, PERUSOPETUKSEN_VUOSILUOKKA, PERUSOPETUKSEN_OPPIAINE, TUVA, TELMA,
    PERUSOPETUKSEN_OPISKELUOIKEUS, AMMATILLINEN_OPISKELUOIKEUS, GENEERINEN_OPISKELUOIKEUS
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
          LOG.info(s"Ei tarvetta tallentaa uutta versiota oppijalle $oppijaNumero, koska haetut tiedot ovat samat kuin kannasta löytyneellä voimassa olevalla versiolla.")
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

  def poistaVersioonLiittyvatEntiteetit(versio: VersioEntiteetti): DBIOAction[_, NoStream, Effect] =
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
       """,
      sqlu"""
          DELETE FROM perusopetuksen_opiskeluoikeudet USING versiot WHERE versiot.tunniste=perusopetuksen_opiskeluoikeudet.versio_tunniste AND versiot.tunniste=${versio.tunniste.toString}::uuid;
       """,
      sqlu"""
          DELETE FROM ammatilliset_opiskeluoikeudet USING versiot WHERE versiot.tunniste=ammatilliset_opiskeluoikeudet.versio_tunniste AND versiot.tunniste=${versio.tunniste.toString}::uuid;
       """,

    ))

  def getAmmatillisenTutkinnonOsaAlueInserts(parentId: Int, suoritus: AmmatillisenTutkinnonOsaAlue): DBIOAction[_, NoStream, Effect] =
    DBIO.sequence(Seq(
      sqlu"""INSERT INTO ammatillisen_tutkinnon_osaalueet(osa_tunniste, nimi, koodi, koodisto, koodistoversio, arvosana, arvosanaasteikko, arvosanaversio, laajuus, laajuuskoodi, laajuuskoodisto, laajuusversio)
            VALUES(${parentId}, ${suoritus.nimi}, ${suoritus.koodi.arvo}, ${suoritus.koodi.koodisto}, ${suoritus.koodi.versio}, ${suoritus.arvosana.map(a => a.arvo)}, ${suoritus.arvosana.map(a => a.koodisto)}, ${suoritus.arvosana.map(a => a.versio)}, ${suoritus.laajuus}, ${suoritus.laajuusKoodi.map(_.arvo)}, ${suoritus.laajuusKoodi.map(_.koodisto)}, ${suoritus.laajuusKoodi.map(_.versio)})"""))

  def getAmmatillisenTutkinnonOsaInserts(parentId: Int, suoritus: AmmatillisenTutkinnonOsa): DBIOAction[_, NoStream, Effect] =
    sql"""INSERT INTO ammatillisen_tutkinnon_osat(tutkinto_tunniste, nimi, koodi, koodisto, koodistoversio, yto, arvosana, arvosanaasteikko, arvosanaversio, laajuus, laajuuskoodi, laajuuskoodisto, laajuusversio)
            VALUES(${parentId}, ${suoritus.nimi}, ${suoritus.koodi.arvo}, ${suoritus.koodi.koodisto}, ${suoritus.koodi.versio}, ${suoritus.yto}, ${suoritus.arvosana.map(a => a.arvo)}, ${suoritus.arvosana.map(a => a.koodisto)}, ${suoritus.arvosana.map(a => a.versio)}, ${suoritus.laajuus}, ${suoritus.laajuusKoodi.map(_.arvo)}, ${suoritus.laajuusKoodi.map(_.koodisto)}, ${suoritus.laajuusKoodi.map(_.versio)}) RETURNING tunniste""".as[(Int)].flatMap(osaTunnisteet => {
      DBIO.sequence(osaTunnisteet.map(tunniste => suoritus.osaAlueet.map(osa => getAmmatillisenTutkinnonOsaAlueInserts(tunniste, osa))).flatten)
    })

  def getAmmatillinenTutkintoInserts(versio: VersioEntiteetti, parentOpiskeluoikeusId: Int, suoritus: AmmatillinenTutkinto): DBIOAction[_, NoStream, Effect] =
    sql"""INSERT INTO ammatilliset_tutkinnot(versio_tunniste, opiskeluoikeus_tunniste, nimi, tyyppi, koodisto, koodistoversio, tila, tilakoodisto, tilaversio, suoritustapa, suoritustapakoodisto, suoritustapaversio, keskiarvo, vahvistuspaivamaara)
            VALUES(
            ${versio.tunniste.toString}::uuid,
            $parentOpiskeluoikeusId,
            ${suoritus.nimi},
            ${suoritus.tyyppi.arvo},
            ${suoritus.tyyppi.koodisto},
            ${suoritus.tyyppi.versio},
            ${suoritus.tila.arvo},
            ${suoritus.tila.koodisto},
            ${suoritus.tila.versio},
            ${suoritus.suoritustapa.arvo},
            ${suoritus.suoritustapa.koodisto},
            ${suoritus.suoritustapa.versio},
            ${suoritus.keskiarvo},
            ${suoritus.vahvistusPaivamaara.map(d => d.toString)}::date
            ) RETURNING tunniste""".as[(Int)]
      .flatMap(tutkintoTunnisteet => {
        DBIO.sequence(tutkintoTunnisteet.map(tunniste => suoritus.osat.map(osa => getAmmatillisenTutkinnonOsaInserts(tunniste, osa))).flatten)
    })



  def getPerusopetuksenVuosiluokkaInserts(versio: VersioEntiteetti, parentOpiskeluoikeusId: Int, suoritus: PerusopetuksenVuosiluokka): DBIOAction[_, NoStream, Effect] =
    DBIO.sequence(Seq(
      sqlu"""INSERT INTO perusopetuksen_vuosiluokat(versio_tunniste, opiskeluoikeus_tunniste, nimi, koodi, alkamispaiva)
            VALUES(${versio.tunniste.toString}::uuid, $parentOpiskeluoikeusId, ${suoritus.nimi}, ${suoritus.koodi}, ${suoritus.alkamisPaiva.map(d => d.toString)}::date)"""))

  def getNuortenPerusopetuksenOppiaineenOppimaaraInserts(versio: VersioEntiteetti, parentOpiskeluoikeusId: Int, suoritus: NuortenPerusopetuksenOppiaineenOppimaara): DBIOAction[_, NoStream, Effect] =
    DBIO.sequence(Seq(
      sqlu"""INSERT INTO nuorten_perusopetuksen_oppiaineen_oppimaarat(versio_tunniste, opiskeluoikeus_tunniste, nimi, koodi, arvosana)
            VALUES(${versio.tunniste.toString}::uuid, $parentOpiskeluoikeusId, ${suoritus.nimi}, ${suoritus.koodi}, ${suoritus.arvosana})"""))

  def getPerusopetuksenOppimaaranAineInserts(parentId: Int, suoritus: PerusopetuksenOppiaine): DBIOAction[_, NoStream, Effect] =
    DBIO.sequence(Seq(
      sqlu"""INSERT INTO perusopetuksen_oppiaineet(oppimaara_tunniste, nimi, koodi, arvosana)
            VALUES(${parentId}, ${suoritus.nimi}, ${suoritus.koodi}, ${suoritus.arvosana})"""))

  def getPerusopetuksenOppimaaraInserts(versio: VersioEntiteetti, parentOpiskeluoikeusId: Int, suoritus: PerusopetuksenOppimaara): DBIOAction[_, NoStream, Effect] =
    sql"""INSERT INTO perusopetuksen_oppimaarat(versio_tunniste, opiskeluoikeus_tunniste, organisaatio_oid, tila, tilakoodisto, tilaversio, vahvistuspaivamaara)
            VALUES(${versio.tunniste.toString}::uuid, $parentOpiskeluoikeusId, ${suoritus.organisaatioOid}, ${suoritus.tila.arvo}, ${suoritus.tila.koodisto},
            ${suoritus.tila.versio}, ${suoritus.vahvistusPaivamaara.map(d => d.toString)}::date) RETURNING tunniste""".as[(Int)]
      .flatMap(oppimaaraTunnisteet => {DBIO.sequence(oppimaaraTunnisteet.map(tunniste => suoritus.aineet.map(osa => getPerusopetuksenOppimaaranAineInserts(tunniste, osa))).flatten)
    })

  def getPerusopetuksenOpiskeluoikeudenSuoritusInserts(versio: VersioEntiteetti, parentOpiskeluoikeusId: Int, suoritus: Suoritus) = {
    suoritus match {
      case s: PerusopetuksenOppimaara => getPerusopetuksenOppimaaraInserts(versio, parentOpiskeluoikeusId, s)
      case s: NuortenPerusopetuksenOppiaineenOppimaara => getNuortenPerusopetuksenOppiaineenOppimaaraInserts(versio, parentOpiskeluoikeusId, s)
      case s: PerusopetuksenVuosiluokka => getPerusopetuksenVuosiluokkaInserts(versio, parentOpiskeluoikeusId, s)
      case _ =>
        LOG.error(s"Tuntematon perusopetuksen suoritustyyppi!")
        throw new Exception(s"Tuntematon perusopetuksen suoritustyyppi")
    }
  }

  def getAmmatillisenOpiskeluoikeudenSuoritusInserts(versio: VersioEntiteetti, parentOpiskeluoikeusId: Int, suoritus: Suoritus) = {
    suoritus match {
      case s: AmmatillinenTutkinto => getAmmatillinenTutkintoInserts(versio, parentOpiskeluoikeusId, s)
      case s: Telma => getTelmaInserts(versio, parentOpiskeluoikeusId, s)
      case unknown =>
        LOG.error(s"Tuntematon ammatillisen suoritustyyppi $unknown!")
        throw new Exception(s"Tuntematon perusopetuksen suoritustyyppi")
    }
  }

  def getAmmatillinenOpiskeluoikeusInserts(versio: VersioEntiteetti, opiskeluoikeus: AmmatillinenOpiskeluoikeus) = {
    sql"""INSERT INTO ammatilliset_opiskeluoikeudet(versio_tunniste, oid, oppilaitos_oid, tila)
          VALUES(${versio.tunniste.toString}::uuid, ${opiskeluoikeus.oid}, ${opiskeluoikeus.oppilaitosOid},
          ${MAPPER.writeValueAsString(opiskeluoikeus.tila)}::jsonb) RETURNING tunniste""".as[(Int)].flatMap(opiskeluoikeusTunnisteet => {
      DBIO.sequence(opiskeluoikeusTunnisteet.flatMap(tunniste => opiskeluoikeus.suoritukset.map(aliSuoritus => getAmmatillisenOpiskeluoikeudenSuoritusInserts(versio, tunniste, aliSuoritus))))
    })
  }

  def getPerusopetuksenOpiskeluoikeusInserts(versio: VersioEntiteetti, opiskeluoikeus: PerusopetuksenOpiskeluoikeus) = {
    sql"""INSERT INTO perusopetuksen_opiskeluoikeudet(versio_tunniste, oid, oppilaitos_oid, lisatiedot, tila)
          VALUES(${versio.tunniste.toString}::uuid, ${opiskeluoikeus.oid}, ${opiskeluoikeus.oppilaitosOid}, ${MAPPER.writeValueAsString(opiskeluoikeus.lisatiedot)}::jsonb,
          ${MAPPER.writeValueAsString(opiskeluoikeus.tila)}::jsonb) RETURNING tunniste""".as[(Int)].flatMap(opiskeluoikeusTunnisteet => {
      DBIO.sequence(opiskeluoikeusTunnisteet.flatMap(tunniste => opiskeluoikeus.suoritukset.map(aliSuoritus => getPerusopetuksenOpiskeluoikeudenSuoritusInserts(versio, tunniste, aliSuoritus))))
    })
  }

  def getGenericOpiskeluoikeusInserts(versio: VersioEntiteetti, opiskeluoikeus: GeneerinenOpiskeluoikeus) = {
    sql"""INSERT INTO geneeriset_opiskeluoikeudet(versio_tunniste, oid, tyyppi, oppilaitos_oid, tila)
          VALUES(${versio.tunniste.toString}::uuid, ${opiskeluoikeus.oid}, ${opiskeluoikeus.tyyppi}, ${opiskeluoikeus.oppilaitosOid},
          ${MAPPER.writeValueAsString(opiskeluoikeus.tila)}::jsonb) RETURNING tunniste""".as[(Int)].flatMap(opiskeluoikeusTunnisteet => {
      DBIO.sequence(opiskeluoikeusTunnisteet.flatMap(tunniste => opiskeluoikeus.suoritukset.map(aliSuoritus => getGenericSuoritusInserts(versio, tunniste, aliSuoritus))))
    })
  }

  def getTuvaInserts(versio: VersioEntiteetti, parentOpiskeluoikeusId: Int, suoritus: Tuva): DBIOAction[_, NoStream, Effect] =
    DBIO.sequence(Seq(
      sqlu"""INSERT INTO tuvat(versio_tunniste, opiskeluoikeus_tunniste, koodi, vahvistuspaivamaara)
            VALUES(${versio.tunniste.toString}::uuid, $parentOpiskeluoikeusId, ${suoritus.koodi}, ${suoritus.vahvistusPaivamaara.map(d => d.toString)}::date)"""))

  def getTelmaInserts(versio: VersioEntiteetti, parentOpiskeluoikeusId: Int, suoritus: Telma): DBIOAction[_, NoStream, Effect] =
    DBIO.sequence(Seq(
      sqlu"""INSERT INTO telmat(versio_tunniste, opiskeluoikeus_tunniste, koodi)
            VALUES(${versio.tunniste.toString}::uuid, $parentOpiskeluoikeusId, ${suoritus.koodi})"""))

  def getOpiskeluoikeusInserts(versio: VersioEntiteetti, opiskeluoikeudet: Set[Opiskeluoikeus]): Set[DBIOAction[Vector[Any], NoStream, Effect & Effect]] = {
    opiskeluoikeudet.map {
      case po: PerusopetuksenOpiskeluoikeus => getPerusopetuksenOpiskeluoikeusInserts(versio, po)
      case ao: AmmatillinenOpiskeluoikeus => getAmmatillinenOpiskeluoikeusInserts(versio, ao)
      case o: GeneerinenOpiskeluoikeus => getGenericOpiskeluoikeusInserts(versio, o)
      case unknown =>
        LOG.error(s"Tuntematon opiskeluoikeustyyppi: $unknown")
        ???
    }
  }

  def getGenericSuoritusInserts(versio: VersioEntiteetti, parentOpiskeluoikeusId: Int, suoritus: Suoritus) = {
    suoritus match {
      case s: Tuva => getTuvaInserts(versio, parentOpiskeluoikeusId, s)
      case s =>
        LOG.error(s"Tuntematon geneerinen suoritus: $s")
        ???
    }
  }

  def tallennaVersioonLiittyvatEntiteetit(versio: VersioEntiteetti, opiskeluoikeudet: Set[Opiskeluoikeus], suoritukset: Set[Suoritus]) = {
    LOG.info(s"Tallennetaan versioon $versio liittyvät opiskeluoikeudet (${opiskeluoikeudet.size}) ja suoritukset (${suoritukset.size})")
    val poistaSuoritukset = poistaVersioonLiittyvatEntiteetit(versio)
    val otaVersioKayttoon = sqlu"""UPDATE versiot SET use_versio_tunniste=NULL WHERE tunniste=${versio.tunniste.toString}::uuid"""

    //Todo, onko meillä ylipäätään suorituksia ilman opiskeluoikeuksia?
    //Koski-data tallennetaan aina käärittynä opiskeluoikeuksiin, mutta virta/ytr-datalle voidaan haluta tehdä jotain muuta.
    //val suoritusInserts = getSuoritusInserts(versio, suoritukset)
    val opiskeluoikeusInserts = getOpiskeluoikeusInserts(versio, opiskeluoikeudet)

    Await.result(db.run(DBIO.sequence(Seq(poistaSuoritukset, DBIO.sequence(opiskeluoikeusInserts), otaVersioKayttoon)).transactionally), DB_TIMEOUT)
  }

  private def haeSuorituksetInternal(versioTunnisteetQuery: slick.jdbc.SQLActionBuilder): Map[VersioEntiteetti, Set[Opiskeluoikeus]] =
    var ammatillisenTutkinnonOsaAlueet: Map[String, Seq[AmmatillisenTutkinnonOsaAlue]] = Map.empty
    var ammatillisenTutkinnonOsat: Map[String, Seq[AmmatillisenTutkinnonOsa]] = Map.empty
    var perusopetuksenOppiaineet: Map[String, Seq[PerusopetuksenOppiaine]] = Map.empty
    var perusopetusByOpiskeluoikeus: Map[String, Seq[Suoritus]] = Map.empty
    var ammatillinenByOpiskeluoikeus: Map[String, Seq[Suoritus]] = Map.empty
    var geneerinenByOpiskeluoikeus: Map[String, Seq[Suoritus]] = Map.empty
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
            w_perusopetuksen_opiskeluoikeudet AS (
              SELECT
                0 AS priority,
                ${PERUSOPETUKSEN_OPISKELUOIKEUS.toString} as tyyppi,
                perusopetuksen_opiskeluoikeudet.tunniste AS tunniste,
                null::int AS parent_tunniste,
                null::int AS parent_opiskeluoikeus_tunniste,
                jsonb_build_object(
                  'oid', perusopetuksen_opiskeluoikeudet.oid,
                  'oppilaitosOid', oppilaitos_oid,
                  'lisatiedot', lisatiedot,
                  'tila', tila
                )::text AS data,
                w_versiot.versio AS versio
                FROM perusopetuksen_opiskeluoikeudet
                INNER JOIN w_versiot ON w_versiot.tunniste=perusopetuksen_opiskeluoikeudet.versio_tunniste),
            w_ammatilliset_opiskeluoikeudet AS (
              SELECT
                0 AS priority,
                ${AMMATILLINEN_OPISKELUOIKEUS.toString} as tyyppi,
                ammatilliset_opiskeluoikeudet.tunniste AS tunniste,
                null::int AS parent_tunniste,
                null::int AS parent_opiskeluoikeus_tunniste,
                jsonb_build_object(
                  'oid', ammatilliset_opiskeluoikeudet.oid,
                  'oppilaitosOid', oppilaitos_oid,
                  'tila', ammatilliset_opiskeluoikeudet.tila
                )::text AS data,
                w_versiot.versio AS versio
                FROM ammatilliset_opiskeluoikeudet
                INNER JOIN w_versiot ON w_versiot.tunniste=ammatilliset_opiskeluoikeudet.versio_tunniste),
            w_geneeriset_opiskeluoikeudet AS (
              SELECT
                0 AS priority,
                ${GENEERINEN_OPISKELUOIKEUS.toString} as tyyppi,
                geneeriset_opiskeluoikeudet.tunniste AS tunniste,
                null::int AS parent_tunniste,
                null::int AS parent_opiskeluoikeus_tunniste,
                jsonb_build_object(
                  'oid', geneeriset_opiskeluoikeudet.oid,
                  'tyyppi', geneeriset_opiskeluoikeudet.tyyppi,
                  'oppilaitosOid', geneeriset_opiskeluoikeudet.oppilaitos_oid,
                  'tila', geneeriset_opiskeluoikeudet.tila
                )::text AS data,
                w_versiot.versio AS versio
                FROM geneeriset_opiskeluoikeudet
                INNER JOIN w_versiot ON w_versiot.tunniste=geneeriset_opiskeluoikeudet.versio_tunniste),
            w_ammatilliset_tutkinnot AS (
              SELECT
                1 AS priority,
                ${AMMATILLINEN_TUTKINTO.toString} AS tyyppi,
                ammatilliset_tutkinnot.tunniste AS tunniste,
                null::int AS parent_tunniste,
                ammatilliset_opiskeluoikeudet.tunniste AS parent_opiskeluoikeus_tunniste,
                jsonb_build_object(
                  'nimi', nimi,
                  'tyyppi', jsonb_build_object('arvo', tyyppi, 'koodisto', koodisto, 'versio', koodistoversio),
                  'tila', jsonb_build_object('arvo', ammatilliset_tutkinnot.tila, 'koodisto', tilakoodisto, 'versio', tilaversio),
                  'vahvistusPaivamaara', vahvistuspaivamaara,
                  'keskiarvo', keskiarvo,
                  'suoritustapa', jsonb_build_object('arvo', suoritustapa, 'koodisto', suoritustapakoodisto, 'versio', suoritustapaversio)
                )::text AS data,
                w_versiot.versio AS versio
              FROM ammatilliset_tutkinnot
              INNER JOIN w_versiot ON w_versiot.tunniste=ammatilliset_tutkinnot.versio_tunniste
              INNER JOIN ammatilliset_opiskeluoikeudet ON ammatilliset_opiskeluoikeudet.tunniste=ammatilliset_tutkinnot.opiskeluoikeus_tunniste),
            w_ammatillisen_tutkinnon_osat AS (
              SELECT
                2 AS priority,
                ${AMMATILLISEN_TUTKINNON_OSA.toString} AS tyyppi,
                ammatillisen_tutkinnon_osat.tunniste AS tunniste,
                tutkinto_tunniste AS parent_tunniste,
                null::int AS parent_opiskeluoikeus_tunniste,
                jsonb_build_object(
                  'nimi', nimi,
                  'koodi', jsonb_build_object('arvo', koodi, 'koodisto', koodisto, 'versio', koodistoversio),
                  'yto', yto,
                  'arvosana', CASE WHEN arvosana IS NOT NULL THEN jsonb_build_object('arvo', arvosana, 'koodisto', arvosanaasteikko, 'versio', arvosanaversio) ELSE null END,
                  'laajuus', laajuus,
                  'laajuusKoodi', CASE WHEN laajuuskoodi IS NOT NULL THEN jsonb_build_object('arvo', laajuuskoodi, 'koodisto', laajuuskoodisto, 'versio', laajuusversio) ELSE NULL END
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
                null::int AS parent_opiskeluoikeus_tunniste,
                jsonb_build_object(
                  'nimi', nimi,
                  'koodi', jsonb_build_object('arvo', koodi, 'koodisto', koodisto, 'versio', koodistoversio),
                  'arvosana', CASE WHEN arvosana IS NOT NULL THEN jsonb_build_object('arvo', arvosana, 'koodisto', arvosanaasteikko, 'versio', arvosanaversio) ELSE null END,
                  'laajuus', laajuus,
                  'laajuusKoodi', CASE WHEN laajuuskoodi IS NOT NULL THEN jsonb_build_object('arvo', laajuuskoodi, 'koodisto', laajuuskoodisto, 'versio', laajuusversio) ELSE NULL END
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
                perusopetuksen_opiskeluoikeudet.tunniste AS parent_opiskeluoikeus_tunniste,
                jsonb_build_object(
                  'organisaatioOid', organisaatio_oid,
                  'tila', jsonb_build_object('arvo', perusopetuksen_oppimaarat.tila, 'koodisto', tilakoodisto, 'versio', tilaversio),
                  'vahvistusPaivamaara', vahvistuspaivamaara
                )::text AS data,
                w_versiot.versio AS versio
              FROM perusopetuksen_oppimaarat
              INNER JOIN w_versiot ON w_versiot.tunniste=perusopetuksen_oppimaarat.versio_tunniste
              INNER JOIN perusopetuksen_opiskeluoikeudet ON perusopetuksen_opiskeluoikeudet.tunniste=perusopetuksen_oppimaarat.opiskeluoikeus_tunniste),
            w_nuorten_perusopetuksen_oppiaineen_oppimaarat AS (
              SELECT
                5 AS priority,
                ${NUORTEN_PERUSOPETUKSEN_OPPIAINEEN_OPPIMAARA.toString} AS tyyppi,
                null::int AS tunniste,
                null::int AS parent_tunniste,
                perusopetuksen_opiskeluoikeudet.tunniste AS parent_opiskeluoikeus_tunniste,
                jsonb_build_object('nimi', nimi, 'koodi', koodi, 'arvosana', arvosana)::text AS data,
                w_versiot.versio AS versio
              FROM nuorten_perusopetuksen_oppiaineen_oppimaarat
              INNER JOIN w_versiot ON w_versiot.tunniste=nuorten_perusopetuksen_oppiaineen_oppimaarat.versio_tunniste
              INNER JOIN perusopetuksen_opiskeluoikeudet ON perusopetuksen_opiskeluoikeudet.tunniste=nuorten_perusopetuksen_oppiaineen_oppimaarat.opiskeluoikeus_tunniste),
            w_perusopetuksen_vuosiluokat AS (
              SELECT
                6 AS priority,
                ${PERUSOPETUKSEN_VUOSILUOKKA.toString} AS tyyppi,
                null::int AS tunniste,
                null::int AS parent_tunniste,
                perusopetuksen_opiskeluoikeudet.tunniste AS parent_opiskeluoikeus_tunniste,
                jsonb_build_object(
                  'nimi', nimi,
                  'koodi', koodi,
                  'alkamisPaiva', alkamispaiva
                )::text AS data,
                w_versiot.versio AS versio
              FROM perusopetuksen_vuosiluokat
              INNER JOIN w_versiot ON w_versiot.tunniste=perusopetuksen_vuosiluokat.versio_tunniste
              INNER JOIN perusopetuksen_opiskeluoikeudet ON perusopetuksen_opiskeluoikeudet.tunniste=perusopetuksen_vuosiluokat.opiskeluoikeus_tunniste),
            w_perusopetuksen_oppiaineet AS (
              SELECT
                7 AS priority,
                ${PERUSOPETUKSEN_OPPIAINE.toString} AS tyyppi,
                null::int AS tunniste,
                oppimaara_tunniste AS parent_tunniste,
                null::int AS parent_opiskeluoikeus_tunniste,
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
                geneeriset_opiskeluoikeudet.tunniste AS parent_opiskeluoikeus_tunniste,
                jsonb_build_object('koodi', koodi, 'vahvistusPaivamaara', vahvistuspaivamaara)::text AS data,
                w_versiot.versio AS versio
              FROM tuvat
              INNER JOIN w_versiot ON w_versiot.tunniste=tuvat.versio_tunniste
              INNER JOIN geneeriset_opiskeluoikeudet ON geneeriset_opiskeluoikeudet.tunniste=tuvat.opiskeluoikeus_tunniste),
            w_telmat AS (
              SELECT
                9 AS priority,
                ${TELMA.toString} AS tyyppi,
                null::int AS tunniste,
                null::int AS parent_tunniste,
                ammatilliset_opiskeluoikeudet.tunniste AS parent_opiskeluoikeus_tunniste,
                jsonb_build_object('koodi', koodi)::text AS data,
                w_versiot.versio AS versio
              FROM telmat
              INNER JOIN w_versiot ON w_versiot.tunniste=telmat.versio_tunniste
              INNER JOIN ammatilliset_opiskeluoikeudet ON ammatilliset_opiskeluoikeudet.tunniste=telmat.opiskeluoikeus_tunniste)
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
          UNION ALL
          SELECT * FROM w_perusopetuksen_opiskeluoikeudet
          UNION ALL
          SELECT * FROM w_ammatilliset_opiskeluoikeudet
          UNION ALL
          SELECT * FROM w_geneeriset_opiskeluoikeudet
          ORDER BY priority DESC;
        """).as[(Int, String, String, String, String, String, String)]), DB_TIMEOUT).flatMap((_, tyyppi, tunniste, parentTunniste, opiskeluoikeusTunniste, data, versioData) =>
        val versio = Option.apply(versioData).map(data => MAPPER.readValue(data, classOf[VersioEntiteetti]))
        KantaEntiteetit.valueOf(tyyppi) match
          case AMMATILLINEN_TUTKINTO =>
            val suoritus = MAPPER.readValue(data, classOf[AmmatillinenTutkinto]).copy(osat = ammatillisenTutkinnonOsat.getOrElse(tunniste, Seq.empty).toSet)
            ammatillinenByOpiskeluoikeus = ammatillinenByOpiskeluoikeus.updatedWith(opiskeluoikeusTunniste) {
              case Some(existingSuoritukset) =>
                Some(suoritus +: existingSuoritukset)
              case None =>
                Some(Seq(suoritus))
            }
            None
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
          case AMMATILLINEN_OPISKELUOIKEUS =>
            val opiskeluoikeus = MAPPER.readValue(data, classOf[AmmatillinenOpiskeluoikeus]).copy(suoritukset = ammatillinenByOpiskeluoikeus.getOrElse(tunniste, Seq.empty))
            Some(versio.get -> opiskeluoikeus)
          case PERUSOPETUKSEN_OPISKELUOIKEUS =>
            val opiskeluoikeus = MAPPER.readValue(data, classOf[PerusopetuksenOpiskeluoikeus]).copy(suoritukset = perusopetusByOpiskeluoikeus.getOrElse(tunniste, Seq.empty))
            Some(versio.get -> opiskeluoikeus)
          case GENEERINEN_OPISKELUOIKEUS =>
            val opiskeluoikeus = MAPPER.readValue(data, classOf[GeneerinenOpiskeluoikeus]).copy(suoritukset = geneerinenByOpiskeluoikeus.getOrElse(tunniste, Seq.empty))
            Some(versio.get -> opiskeluoikeus)
          case PERUSOPETUKSEN_OPPIMAARA =>
            val suoritus = MAPPER.readValue(data, classOf[PerusopetuksenOppimaara]).copy(aineet = perusopetuksenOppiaineet.getOrElse(tunniste, Seq.empty).toSet)
            perusopetusByOpiskeluoikeus = perusopetusByOpiskeluoikeus.updatedWith(opiskeluoikeusTunniste) {
              case Some(existingSuoritukset) => Some(suoritus +: existingSuoritukset)
              case None => Some(Seq(suoritus))
            }
            None //Nämä tulevat PerusopetuksenOpiskeluoikeuteen käärittyinä suorituksina
          case NUORTEN_PERUSOPETUKSEN_OPPIAINEEN_OPPIMAARA =>
            val suoritus = MAPPER.readValue(data, classOf[NuortenPerusopetuksenOppiaineenOppimaara])
            perusopetusByOpiskeluoikeus = perusopetusByOpiskeluoikeus.updatedWith(opiskeluoikeusTunniste) {
              case Some(existingSuoritukset) => Some(suoritus +: existingSuoritukset)
              case None => Some(Seq(suoritus))
            }
            None //Nämä tulevat PerusopetuksenOpiskeluoikeuteen käärittyinä suorituksina
          case PERUSOPETUKSEN_VUOSILUOKKA =>
            val suoritus = MAPPER.readValue(data, classOf[PerusopetuksenVuosiluokka])
            perusopetusByOpiskeluoikeus = perusopetusByOpiskeluoikeus.updatedWith(opiskeluoikeusTunniste) {
              case Some(existingSuoritukset) => Some(suoritus +: existingSuoritukset)
              case None => Some(Seq(suoritus))
            }
            None //Nämä tulevat PerusopetuksenOpiskeluoikeuteen käärittyinä suorituksina
          case TUVA =>
            val suoritus = MAPPER.readValue(data, classOf[Tuva])
            geneerinenByOpiskeluoikeus = geneerinenByOpiskeluoikeus.updatedWith(opiskeluoikeusTunniste) {
              case Some(existingSuoritukset) => Some(suoritus +: existingSuoritukset)
              case None => Some(Seq(suoritus))
            }
            None //Nämä tulevat GeneeriseenOpiskeluoikeuteen käärittyinä suorituksina
          case TELMA =>
            val suoritus = MAPPER.readValue(data, classOf[Telma])
            ammatillinenByOpiskeluoikeus = ammatillinenByOpiskeluoikeus.updatedWith(opiskeluoikeusTunniste) {
              case Some(existingSuoritukset) => Some(suoritus +: existingSuoritukset)
              case None => Some(Seq(suoritus))
            }
            None //Nämä tulevat AmmatilliseenOpiskeluoikeuteen käärittyinä suorituksina
          case PERUSOPETUKSEN_OPPIAINE =>
            val osa = MAPPER.readValue(data, classOf[PerusopetuksenOppiaine])
            perusopetuksenOppiaineet = perusopetuksenOppiaineet.updatedWith(parentTunniste)(osat => osat match
              case Some(osat) => Some(osa +: osat)
              case None => Some(Seq(osa))
            )
            None
          case default =>
            None)
      .groupBy((versio, _) => versio)
      .map((versio, suoritukset) => (versio, suoritukset.map(t => t._2).toSet))

  def haeSuoritukset(oppijaNumero: String): Map[VersioEntiteetti, Set[Opiskeluoikeus]] =
    haeSuorituksetInternal(sql"""SELECT tunniste FROM versiot WHERE oppijanumero=${oppijaNumero} AND upper(voimassaolo)='infinity'::timestamptz""")
}

package fi.oph.suorituspalvelu.business

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.oph.suorituspalvelu.business.KantaOperaatiot.{MAPPER, XMLMAPPER}
import org.skyscreamer.jsonassert.{JSONCompare, JSONCompareMode}
import slick.jdbc.{GetResult, JdbcBackend, SQLActionBuilder, SetParameter}
import slick.jdbc.PostgresProfile.api.*
import com.github.tminglei.slickpg.utils.PlainSQLUtils.mkArraySetParameter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import org.slf4j.LoggerFactory

import java.time.{Instant, LocalDate}
import java.util.UUID
import com.fasterxml.jackson.dataformat.xml.XmlMapper

implicit val setStringArray: SetParameter[Seq[String]] = mkArraySetParameter[String]("varchar")

implicit val strList: GetResult[List[String]] = GetResult[List[String]](r =>
  val array = r.rs.getArray(r.skip.currentPos)
  if(array==null)
    List.empty
  else
    array.getArray
      .asInstanceOf[Array[Any]]
      .toList
      .map(_.toString())
)

object KantaOperaatiot {
  val MAPPER: ObjectMapper = {
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper.registerModule(new JavaTimeModule())
    mapper.registerModule(new Jdk8Module()) // tämä on java.util.Optional -kenttiä varten
    mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, true)
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
    mapper.registerSubtypes(
      classOf[PerusopetuksenOpiskeluoikeus],
      classOf[PerusopetuksenOppimaara],
      classOf[AmmatillinenOpiskeluoikeus],
      classOf[VirtaOpiskeluoikeus],
      classOf[PoistettuOpiskeluoikeus],
      classOf[AmmatillinenPerustutkinto],
      classOf[AmmattiTutkinto],
      classOf[GeneerinenOpiskeluoikeus],
      classOf[YOOpiskeluoikeus],
      classOf[Telma],
      classOf[PerusopetuksenOppimaaranOppiaineidenSuoritus],
      classOf[Tuva],
      classOf[VirtaTutkinto],
      classOf[Opintosuoritus],
      classOf[VapaaSivistystyo],
      classOf[EBTutkinto],
      classOf[ErikoisAmmattiTutkinto],
      classOf[LukionOppimaara])
    mapper
  }

  val XMLMAPPER: ObjectMapper = {
    val mapper = new XmlMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper.registerModule(new JavaTimeModule())
    mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    mapper.configure(SerializationFeature.INDENT_OUTPUT, true)
    mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
    mapper
  }

  enum KantaEntiteetit:
    case AMMATILLINEN_TUTKINTO, AMMATILLISEN_TUTKINNON_OSA, AMMATILLISEN_TUTKINNON_OSAALUE, PERUSOPETUKSEN_OPPIMAARA,
    NUORTEN_PERUSOPETUKSEN_OPPIAINEEN_OPPIMAARA, PERUSOPETUKSEN_VUOSILUOKKA, PERUSOPETUKSEN_OPPIAINE, TUVA, TELMA,
    YOTUTKINTO, PERUSOPETUKSEN_OPISKELUOIKEUS, AMMATILLINEN_OPISKELUOIKEUS, GENEERINEN_OPISKELUOIKEUS
}

class KantaOperaatiot(db: JdbcBackend.JdbcDatabaseDef) {

  final val DB_TIMEOUT = 30.seconds
  val LOG = LoggerFactory.getLogger(classOf[KantaOperaatiot])

  def getUUID(): UUID =
    UUID.randomUUID()

  private def isNewVersion(henkiloOid: String, lahdeJarjestelma: Lahdejarjestelma, lahdeTunniste: String, jsonData: Seq[String], xmlData: Seq[String], fetchedAt: Instant): DBIOAction[Boolean, NoStream, Effect] =
    sql"""
      SELECT to_json(lower(voimassaolo)::timestamptz)#>>'{}' as alku, data_json, data_xml
      FROM versiot
      WHERE henkilo_oid=${henkiloOid}
        AND lahdejarjestelma=${lahdeJarjestelma.nimi}
        AND lahdetunniste=${lahdeTunniste}
        AND upper(voimassaolo)='infinity'::timestamptz
    """.as[(String, Seq[String], Seq[String])].map(result => {
      if (result.isEmpty)
        true
      else
        val (alku, existingJsonData, existingXmlData) = result.head
        if (fetchedAt.toEpochMilli <= Instant.parse(alku).toEpochMilli)
          LOG.info(s"Ei tarvetta tallentaa uutta versiota henkilölle $henkiloOid, koska aikaisemmin tallennettu versio on uudempi.")
          false
        else
          val isNewXmlData = existingXmlData.length != xmlData.length ||
            existingXmlData.sorted.zip(xmlData.sorted).exists((existingDataItem, dataItem) => {
              val existingDataAsJson = MAPPER.writeValueAsString(XMLMAPPER.readValue(existingDataItem, classOf[Map[Any, Any]]))
              val dataAsJson = MAPPER.writeValueAsString(XMLMAPPER.readValue(dataItem, classOf[Map[Any, Any]]))
              !JSONCompare.compareJSON(existingDataAsJson, dataAsJson, JSONCompareMode.NON_EXTENSIBLE).passed()
            })
          val isNewJsonData = existingJsonData.length != jsonData.length ||
            existingJsonData.sorted.zip(jsonData.sorted).exists((existingDataItem, dataItem) => {
              !JSONCompare.compareJSON(existingDataItem, dataItem, JSONCompareMode.NON_EXTENSIBLE).passed()
            })
          if (!isNewXmlData && !isNewJsonData)
            LOG.info(s"Ei tarvetta tallentaa uutta versiota henkilölle $henkiloOid, koska haetut tiedot ovat samat kuin kannasta löytyneellä voimassa olevalla versiolla.")
          isNewXmlData || isNewJsonData
    })

  private def findExistingVersionByLahdeVersio(henkiloOid: String, lahdeJarjestelma: Lahdejarjestelma, lahdeTunniste: String, lahdeVersio: Int): DBIOAction[Option[String], NoStream, Effect] =
    sql"""
      SELECT tunniste::text
      FROM versiot
      WHERE henkilo_oid=${henkiloOid}
        AND lahdejarjestelma=${lahdeJarjestelma.nimi}
        AND lahdetunniste=${lahdeTunniste}
        AND lahdeversio=${lahdeVersio}
    """.as[String].headOption

  private def discontinuePreviousVersionedVersion(
    henkiloOid: String,
    lahdeJarjestelma: Lahdejarjestelma,
    lahdeTunniste: String,
    lahdeVersio: Int,
    timestamp: Instant
  ): DBIOAction[Int, NoStream, Effect] =
    sqlu"""
      UPDATE versiot
      SET voimassaolo=tstzrange(lower(voimassaolo), ${timestamp.toString}::timestamptz)
      WHERE henkilo_oid=${henkiloOid}
        AND lahdejarjestelma=${lahdeJarjestelma.nimi}
        AND lahdetunniste=${lahdeTunniste}
        AND lahdeversio < ${lahdeVersio}
        AND upper(voimassaolo) > ${timestamp.toString}::timestamptz
    """

  private def discontinueCurrentVersion(
    henkiloOid: String,
    lahdeJarjestelma: Lahdejarjestelma,
    lahdeTunniste: String,
    timestamp: Instant
  ): DBIOAction[Int, NoStream, Effect] =
    sqlu"""
      UPDATE versiot
      SET voimassaolo=tstzrange(lower(voimassaolo), ${timestamp.toString}::timestamptz)
      WHERE henkilo_oid=${henkiloOid}
        AND lahdejarjestelma=${lahdeJarjestelma.nimi}
        AND lahdetunniste=${lahdeTunniste}
        AND upper(voimassaolo) = 'infinity'::timestamptz
    """

  private def insertNewVersion(
                                henkiloOid: String,
                                lahdeJarjestelma: Lahdejarjestelma,
                                jsonData: Seq[String],
                                xmlData: Seq[String],
                                timestamp: Instant,
                                lahdeTunniste: String,
                                lahdeVersio: Option[Int]
  ): DBIOAction[Option[VersioEntiteetti], NoStream, Effect] =
    val tunniste = getUUID()
    LOG.info(s"Luodaan uusi versio $tunniste henkilölle $henkiloOid (lahdeVersio=$lahdeVersio)")
    sql"""
      WITH subsequent_version AS (
        SELECT lower(voimassaolo) as loppu
        FROM versiot
        WHERE henkilo_oid=${henkiloOid}
          AND lahdejarjestelma=${lahdeJarjestelma.nimi}
          AND lahdetunniste=${lahdeTunniste}
          AND ${lahdeVersio}::integer IS NOT NULL
          AND lahdeversio > ${lahdeVersio}
        ORDER BY lahdeversio ASC
        LIMIT 1
      )
      INSERT INTO versiot(tunniste, henkilo_oid, voimassaolo, lahdejarjestelma, lahdetunniste, lahdeversio, data_json, data_xml)
      VALUES(
        ${tunniste.toString}::uuid,
        ${henkiloOid},
        tstzrange(${timestamp.toString}::timestamptz, COALESCE((SELECT loppu FROM subsequent_version), 'infinity'::timestamptz)),
        ${lahdeJarjestelma.nimi},
        ${lahdeTunniste},
        ${lahdeVersio},
        ${jsonData}::jsonb[],
        ${xmlData}::xml[]
      )
      RETURNING jsonb_build_object(
        'tunniste', tunniste,
        'henkiloOid', henkilo_oid,
        'alku', to_json(lower(voimassaolo)::timestamptz)#>>'{}',
        'loppu', CASE WHEN upper(voimassaolo)='infinity'::timestamptz THEN null ELSE to_json(upper(voimassaolo)::timestamptz)#>>'{}' END,
        'lahdeJarjestelma', lahdejarjestelma,
        'lahdeTunniste', lahdetunniste,
        'lahdeVersio', lahdeversio,
        'parserVersio', parser_versio
      )::text
    """.as[String].head.map(json => Some(MAPPER.readValue(json, classOf[VersioEntiteetti])))

  def tallennaJarjestelmaVersio(henkiloOid: String, lahdeJarjestelma: Lahdejarjestelma, jsonData: Seq[String], xmlData: Seq[String], voimassaolonAlku: Instant, lahdeTunniste: String, lahdeVersio: Option[Int]): Option[VersioEntiteetti] =
    val insertHenkiloAction = sqlu"INSERT INTO henkilot(oid) VALUES (${henkiloOid}) ON CONFLICT DO NOTHING"
    val lockHenkiloAction = sql"""SELECT 1 FROM henkilot WHERE oid=${henkiloOid} FOR UPDATE"""
    val timestamp = Instant.ofEpochMilli(voimassaolonAlku.toEpochMilli)

    // KOSKI is versioned, others are not
    val isVersionedSource = lahdeJarjestelma == Lahdejarjestelma.KOSKI
    if (isVersionedSource && lahdeVersio.isEmpty)
      throw new IllegalArgumentException(s"lahdeVersio vaaditaan versioitavalle lähteelle $lahdeJarjestelma")
    if (!isVersionedSource && lahdeVersio.isDefined)
      throw new IllegalArgumentException(s"lahdeVersio ei sallittu versioimattomalle lähteelle $lahdeJarjestelma")

    val upsertAction = DBIO.sequence(Seq(insertHenkiloAction, lockHenkiloAction.as[Int])).flatMap(_ =>
      lahdeVersio match {
        case Some(lahdeVersio) =>
          // Versioned: check if same lahdeversio already exists
          findExistingVersionByLahdeVersio(henkiloOid, lahdeJarjestelma, lahdeTunniste, lahdeVersio).flatMap {
            case Some(existingTunniste) =>
              LOG.info(s"Versio lahdeVersio=$lahdeVersio henkilölle $henkiloOid on jo olemassa (tunniste=$existingTunniste), ohitetaan.")
              DBIO.successful(None)
            case None =>
              discontinuePreviousVersionedVersion(henkiloOid, lahdeJarjestelma, lahdeTunniste, lahdeVersio, timestamp).flatMap(_ =>
                insertNewVersion(henkiloOid, lahdeJarjestelma, jsonData, xmlData, timestamp, lahdeTunniste, Some(lahdeVersio))
              )
          }
        case None =>
          // Unversioned: check if this is a new version based on timestamp and data comparison
          isNewVersion(henkiloOid, lahdeJarjestelma, lahdeTunniste, jsonData, xmlData, timestamp).flatMap {
            case false =>
              DBIO.successful(None)
            case true =>
              discontinueCurrentVersion(henkiloOid, lahdeJarjestelma, lahdeTunniste, timestamp).flatMap(_ =>
                insertNewVersion(henkiloOid, lahdeJarjestelma, jsonData, xmlData, timestamp, lahdeTunniste, lahdeVersio)
              )
          }
      }
    )
    Await.result(db.run(upsertAction.transactionally), DB_TIMEOUT)

  def haeHenkilonVersiot(henkiloOid: String) = {
    Await.result(db.run(
        sql"""
        SELECT jsonb_build_object(
          'tunniste', versiot.tunniste,
          'henkiloOid', versiot.henkilo_oid,
          'alku',to_json(lower(voimassaolo)::timestamptz)#>>'{}',
          'loppu', CASE WHEN upper(voimassaolo)='infinity'::timestamptz THEN null ELSE to_json(upper(voimassaolo)::timestamptz)#>>'{}' END,
          'lahdeJarjestelma', versiot.lahdejarjestelma,
          'lahdeTunniste', versiot.lahdetunniste,
          'lahdeVersio', versiot.lahdeversio,
          'parserVersio', versiot.parser_versio
        )::text AS versio
        FROM versiot where henkilo_oid = $henkiloOid""".as[String]), DB_TIMEOUT)
      .map(json => MAPPER.readValue(json, classOf[VersioEntiteetti]))
  }

  def haeData(versio: VersioEntiteetti): (VersioEntiteetti, Seq[String], Seq[String]) =
    Await.result(db.run(
      sql"""SELECT jsonb_build_object('tunniste', tunniste,
              'henkiloOid', henkilo_oid,
              'alku',to_json(lower(voimassaolo)::timestamptz)#>>'{}',
              'loppu', CASE WHEN upper(voimassaolo)='infinity'::timestamptz THEN null ELSE to_json(upper(voimassaolo)::timestamptz)#>>'{}' END,
              'lahdeJarjestelma', lahdejarjestelma,
              'lahdeTunniste', lahdetunniste,
              'lahdeVersio', lahdeversio,
              'parserVersio', parser_versio
            )::text AS versio,
            data_json::text[],
            data_xml::text[]
            FROM versiot
            WHERE tunniste=${versio.tunniste.toString}::UUID""".as[(String, Seq[String], Seq[String])]), DB_TIMEOUT)
      .map((json, jsonData, xmlData) => (MAPPER.readValue(json, classOf[VersioEntiteetti]), jsonData, xmlData)).head

  def haeVersiot(lahdeJarjestelma: Lahdejarjestelma): Seq[VersioEntiteetti] =
    Await.result(db.run(
        sql"""SELECT jsonb_build_object('tunniste', tunniste,
              'henkiloOid', henkilo_oid,
              'alku',to_json(lower(voimassaolo)::timestamptz)#>>'{}',
              'loppu', CASE WHEN upper(voimassaolo)='infinity'::timestamptz THEN null ELSE to_json(upper(voimassaolo)::timestamptz)#>>'{}' END,
              'lahdeJarjestelma', lahdejarjestelma,
              'lahdeTunniste', lahdetunniste,
              'lahdeVersio', lahdeversio,
              'parserVersio', parser_versio
            )::text AS versio
            FROM versiot
            WHERE lahdejarjestelma=${lahdeJarjestelma.nimi}""".as[String]), DB_TIMEOUT)
      .map(json => MAPPER.readValue(json, classOf[VersioEntiteetti]))

  def tallennaVersioonLiittyvatEntiteetit(versio: VersioEntiteetti, opiskeluoikeudet: Set[Opiskeluoikeus], lahtokoulut: Seq[Lahtokoulu], parserVersio: Int) = {
    LOG.info(s"Tallennetaan versioon $versio liittyvät opiskeluoikeudet (${opiskeluoikeudet.size}) kpl")
    val lockHenkiloAction = sql"""SELECT 1 FROM henkilot WHERE oid=${versio.henkiloOid} FOR UPDATE""" // ei tarvita inserttiä henkilöt-tauluun, jos on versio niin on myös henkilö
    val updateVersionAction = lockHenkiloAction.as[Int].flatMap(_ =>
      sql"""
           UPDATE versiot
           SET opiskeluoikeudet=${MAPPER.writeValueAsString(Container(opiskeluoikeudet))}::jsonb, parser_versio=${parserVersio}
           WHERE tunniste=${versio.tunniste.toString}::uuid RETURNING upper(voimassaolo)='infinity'::timestamptz""".as[Boolean])
    val updateLahtokoulutAction = updateVersionAction.flatMap(isNewestVersion => {
      // lähtökoulut tallennetaan vain viimeisimmälle versiolle, ts. ne pitää päivittää ainoastaan kun tallennetaan uusin versio (voimassaolon loppu == infinity)
      if(!isNewestVersion.head)
        DBIO.successful(Seq.empty[DBIO[Int]])
      else {
        DBIO.sequence(
          sqlu"""DELETE FROM lahtokoulut WHERE henkilo_oid=${versio.henkiloOid} AND lahdejarjestelma=${versio.lahdeJarjestelma.nimi} AND lahdetunniste=${versio.lahdeTunniste}""" +: lahtokoulut.map(lahtokoulu =>
          sqlu"""
              INSERT INTO lahtokoulut (versio_tunniste, henkilo_oid, lahdejarjestelma, lahdetunniste, suorituksen_alku, suorituksen_loppu, oppilaitos_oid, valmistumisvuosi, luokka, tila, arvosanapuuttuu, suoritustyyppi)
              VALUES(
                ${versio.tunniste.toString}::uuid,
                ${versio.henkiloOid},
                ${versio.lahdeJarjestelma.nimi},
                ${versio.lahdeTunniste},
                ${lahtokoulu.suorituksenAlku.toString}::date,
                ${lahtokoulu.suorituksenLoppu.map(ov => ov.toString).getOrElse(null)}::date,
                ${lahtokoulu.oppilaitosOid},
                ${lahtokoulu.valmistumisvuosi},
                ${lahtokoulu.luokka},
                ${lahtokoulu.tila.map(t => t.toString).orNull},
                ${lahtokoulu.arvosanaPuuttuu},
                ${lahtokoulu.suoritusTyyppi.toString})
              """))
      }
    })
    Await.result(db.run(updateLahtokoulutAction.transactionally), DB_TIMEOUT)
  }

  private def haeSuorituksetInternal(versioTunnisteetQuery: slick.jdbc.SQLActionBuilder): Map[VersioEntiteetti, Set[Opiskeluoikeus]] = {
    Await.result(db.run(
        (sql"""
          WITH w_versiotunnisteet(tunniste) AS ("""
          concat
          versioTunnisteetQuery
          concat
          sql""")
          SELECT
            jsonb_build_object(
              'tunniste', versiot.tunniste,
              'henkiloOid', versiot.henkilo_oid,
              'alku',to_json(lower(versiot.voimassaolo)::timestamptz)#>>'{}',
              'loppu', CASE WHEN upper(voimassaolo)='infinity'::timestamptz THEN null ELSE to_json(upper(voimassaolo)::timestamptz)#>>'{}' END,
              'lahdeJarjestelma', versiot.lahdejarjestelma,
              'lahdeTunniste', versiot.lahdetunniste,
              'lahdeVersio', versiot.lahdeversio,
              'parserVersio', versiot.parser_versio
            )::text AS versio,
            COALESCE(opiskeluoikeudet, '{"opiskeluoikeudet":[]}'::jsonb) AS opiskeluoikeudet
          FROM w_versiotunnisteet JOIN versiot ON w_versiotunnisteet.tunniste=versiot.tunniste;
        """).as[(String, String)]), DB_TIMEOUT).map((versioJson, opiskeluoikeudetJson) => {
        (MAPPER.readValue(versioJson, classOf[VersioEntiteetti]), MAPPER.readValue(opiskeluoikeudetJson, classOf[Container]).opiskeluoikeudet)
      })
      .groupBy((versio, _) => versio)
      .map((versio, tuples) => versio -> tuples.flatMap(_._2).toSet)
  }

  def haeSuorituksetAjanhetkella(henkiloOid: String, timestamp: Instant): Map[VersioEntiteetti, Set[Opiskeluoikeus]] = {
    haeSuorituksetInternal(sql"""SELECT tunniste FROM versiot WHERE henkilo_oid=${henkiloOid} AND ${timestamp.toString}::timestamptz <@ voimassaolo""")
  }

  def haeSuoritukset(henkiloOid: String): Map[VersioEntiteetti, Set[Opiskeluoikeus]] = {
    haeSuorituksetAjanhetkella(henkiloOid, Instant.now())
  }

  def haeVersio(tunniste: UUID): Option[VersioEntiteetti] =
    Await.result(db.run(
        sql"""SELECT jsonb_build_object(
                'tunniste', tunniste,
                'henkiloOid', henkilo_oid,
                'alku',to_json(lower(voimassaolo)::timestamptz)#>>'{}',
                'loppu', CASE WHEN upper(voimassaolo)='infinity'::timestamptz THEN null ELSE to_json(upper(voimassaolo)::timestamptz)#>>'{}' END,
                'lahdeJarjestelma', lahdejarjestelma,
                'lahdeTunniste', lahdetunniste,
                'lahdeVersio', lahdeversio,
                'parserVersio', parser_versio
              )::text AS versio
              FROM versiot
              WHERE tunniste=${tunniste.toString}::UUID""".as[String]), DB_TIMEOUT)
      .map(json => MAPPER.readValue(json, classOf[VersioEntiteetti])).headOption

  /**
   * Palauttaa henkilöt jotka lähettävien katselijalla on oikeus nähdä tarkastusnäkymässä ohjausvelvollisuuden tai jälkitarkastelun (ks. alla)
   * perusteella. Haku perustuu seuraaviin oletuksiin:
   *  - Ohjausvelvollisuudella on alkamispäivä (tämä on tyypillisesti perusteena olevan suorituksen alkamispäivä)
   *  - Ohjausvelvollisuuden perusteena olevan suorituksen valmistumisen tai keskeytymisen jälkeen lähettävien katselijoilla
   *    on oikeus tarkastaa henkilön tilanne loppumisvuotta seuraavan vuoden tammikuun loppuun asti (jälkitarkastelu)
   *  - Jälkitarkastelu ei pääty toisen suorituksen (esim. vuosiluokka, lisäpistekoulutus, lukio, ammatillinen koulutus) aloittamiseen
   *  - Päivämäärä-parametrin avulla tarkastelu voidaan tehdä tietyllä ajanhetkellä, esim perusopetuksen jälkeisen koulutuksen yhteishaun leikkuripäivänä
   *
   * @param paivamaara            ajanhetki jolloin henkilön on pitänyt olla oppilaitoksen ohjausvelvollisuuden tai jälkitarkastelun (päättymisvuotta seuraavan vuoden tammikuun loppuun asti) piirissä,
   *                              tämän avulla näkyminen rajataan opoille päättymisvuotta seuraavan vuoden tammikuun loppuun
   * @param oppilaitosOid         oppilaitoksen tunniste jonka ohjattavia haetaan
   * @param valmistumisVuosi      henkilöiden valmistumisvuosi (tämä on tyyppillisesti perusopetuksen vuosiluokan tai lisäpistekoulutuksen aloitusvuosi + 1)
   * @param luokka                luokka (ei luokka-aste)
   * @param keskenTaiKeskeytynyt  jos tämä true haetaan vain henkilöitä joiden ohjausvelvollisuuden perusteena oleva suoritus on kesken tai keskeytynyt, muutoin haetaan kaikkia
   * @param arvosanaPuuttuu       jos tämä true haetaan vain henkilöitä joiden ohjausvelvollisuuden perusteena on perusopetuksen suorittaminen ja henkilöllä ei ole perusopetuksen oppimäärän suoritusta jolta löytyvät kaikki yhteisten aineiden arvosanat (ei siis tarvitse olla sama suoritus jos henkilö esim. vaihtanut koulua)
   *                              jos false niin haetaan kaikkia
   */
  def haeLahtokoulunOppilaat(paivamaara: Option[LocalDate], oppilaitosOid: String, valmistumisVuosi: Int, luokka: Option[String], keskenTaiKeskeytynyt: Boolean, arvosanaPuuttuu: Boolean, lahtokouluTyypit: Set[LahtokouluTyyppi]): Set[(String, Option[String])] =
    Await.result(db.run(
      sql"""
          WITH
          -- Generoidaan lista ohjausvastuista joilla oikea näkyvyys, ts. päättymispäivää seuraavan vuoden tammikuun loppuun
          henkilot_loppu AS NOT MATERIALIZED (
            SELECT
              henkilo_oid,
              luokka,
              CASE
                WHEN suorituksen_loppu IS NULL THEN 'infinity'::date
                ELSE (SELECT DATE_TRUNC('year', suorituksen_loppu) + INTERVAL '1 year 1 months') -- ohjausvastuun loppua seuraavan vuoden tammikuun loppu
              END as loppu
            FROM lahtokoulut
            WHERE oppilaitos_oid=$oppilaitosOid
            AND valmistumisvuosi=$valmistumisVuosi
            AND (${!luokka.isDefined} OR luokka=$luokka)
            AND (${!keskenTaiKeskeytynyt} OR tila=${SuoritusTila.KESKEN.toString} OR tila=${SuoritusTila.KESKEYTYNYT.toString})
            AND (${!arvosanaPuuttuu} OR arvosanapuuttuu)
            AND suoritustyyppi = ANY(ARRAY[#${lahtokouluTyypit.map(p => s"'$p'").mkString(",")}])
          )
          -- haetaan listasta oppilaitoksen ja vuoden halutun tyyppiset ohjausvastuut
          SELECT henkilo_oid, luokka
          FROM henkilot_loppu
          WHERE ${paivamaara.isEmpty} OR loppu>${paivamaara.getOrElse(LocalDate.now).toString}::date
         """.as[(String, Option[String])]), DB_TIMEOUT).toSet

  def haeLahtokoulut(henkiloOidit: Set[String]): Set[Lahtokoulu] =
    Await.result(db.run(
        (sql"""
          SELECT
            jsonb_build_object(
              'suorituksenAlku', suorituksen_alku,
              'suorituksenLoppu', suorituksen_loppu,
              'oppilaitosOid', oppilaitos_oid,
              'valmistumisvuosi', valmistumisvuosi,
              'luokka', luokka,
              'tila', tila,
              'arvosanaPuuttuu', arvosanapuuttuu,
              'suoritusTyyppi', suoritustyyppi
            )::text AS data
          FROM lahtokoulut
          WHERE henkilo_oid=ANY(${henkiloOidit.toSeq})
        """).as[String]), DB_TIMEOUT)
      .map(data => MAPPER.readValue(data, classOf[Lahtokoulu])).toSet

  def haePKOppilaitokset(lahtokouluTyypit: Set[LahtokouluTyyppi]): Set[String] =
    Await.result(db.run(
      sql"""
          SELECT DISTINCT oppilaitos_oid
          FROM lahtokoulut
          WHERE suoritustyyppi = ANY(ARRAY[#${lahtokouluTyypit.map(p => s"'$p'").mkString(",")}])
        """.as[String]), DB_TIMEOUT).toSet

  def haeVuodet(oppilaitosOid: String, lahtokouluTyypit: Set[LahtokouluTyyppi]): Set[String] =
    Await.result(db.run(
      sql"""
          SELECT DISTINCT valmistumisVuosi
          FROM lahtokoulut
          WHERE oppilaitos_oid=$oppilaitosOid
          AND suoritustyyppi = ANY(ARRAY[#${lahtokouluTyypit.map(p => s"'$p'").mkString(",")}])
        """.as[String]), DB_TIMEOUT).toSet

  def haeLuokat(oppilaitosOid: String, valmistumisVuosi: Int): Set[String] =
    Await.result(db.run(
      sql"""
          SELECT DISTINCT luokka
          FROM lahtokoulut
          WHERE oppilaitos_oid=$oppilaitosOid
          AND valmistumisvuosi=$valmistumisVuosi
        """.as[String]), DB_TIMEOUT).toSet

  def haeHenkilotJaLuokat(oppilaitosOid: String, valmistumisVuosi: Int): Set[(String, String)] =
    Await.result(db.run(
      sql"""
          SELECT DISTINCT henkilo_oid, luokka
          FROM lahtokoulut
          WHERE oppilaitos_oid=$oppilaitosOid
          AND valmistumisvuosi=$valmistumisVuosi
        """.as[(String, String)]), DB_TIMEOUT).toSet

  def paataVersionVoimassaolo(tunniste: UUID): Boolean =
    LOG.info(s"päätetään version $tunniste voimassaolo")
    val voimassaolo = sqlu"""UPDATE versiot SET voimassaolo=tstzrange(lower(voimassaolo), now()) WHERE tunniste=${tunniste.toString}::uuid AND upper(voimassaolo)='infinity'::timestamptz"""
    Await.result(db.run(voimassaolo), DB_TIMEOUT)>0

  def haeHenkilonYliajot(henkiloOid: String, hakuOid: String): Seq[AvainArvoYliajo] = {
    Await.result(db.run(
      sql"""
        SELECT
          avain,
          arvo,
          henkilo_oid,
          haku_oid,
          virkailija_oid,
          selite
        FROM yliajot
        WHERE henkilo_oid = ${henkiloOid}
          AND haku_oid = ${hakuOid}
          AND upper(voimassaolo)='infinity'::timestamptz
        ORDER BY lower(voimassaolo) DESC
      """.as[(String, String, String, String, String, String)]
        .map(rows => rows.map {
          case (avain, arvo, henkiloOid, hakuOid, virkailijaOid, selite) =>
            AvainArvoYliajo(avain, Option.apply(arvo), henkiloOid, hakuOid, virkailijaOid, selite)
        })
    ), DB_TIMEOUT)
  }

  def tallennaYliajot(yliajot: Seq[AvainArvoYliajo], tallennusHetki: Instant = Instant.now): Unit = {

    // Päivitetään mahdollisten vanhojen versioiden voimassaolo loppumaan yliajon alkuhetkeen
    val updateOldVersionsAction = DBIO.sequence(
      yliajot.map { yliajo =>
        sqlu"""
        UPDATE yliajot
        SET voimassaolo = tstzrange(lower(voimassaolo), ${tallennusHetki.toString}::timestamptz)
        WHERE henkilo_oid = ${yliajo.henkiloOid}
          AND haku_oid = ${yliajo.hakuOid}
          AND avain = ${yliajo.avain}
          AND upper(voimassaolo) = 'infinity'::timestamptz
        """
      }
    )

    // Luodaan uudet yliajot
    val insertNewVersionsAction = DBIO.sequence(
      yliajot.map { yliajo =>
        sqlu"""
        INSERT INTO yliajot (
          avain,
          arvo,
          henkilo_oid,
          haku_oid,
          virkailija_oid,
          selite,
          voimassaolo
        ) VALUES (
          ${yliajo.avain},
          ${yliajo.arvo},
          ${yliajo.henkiloOid},
          ${yliajo.hakuOid},
          ${yliajo.virkailijaOid},
          ${yliajo.selite},
          tstzrange(${tallennusHetki.toString}::timestamptz, 'infinity'::timestamptz)
        )
        """
      }
    )

    // Suoritetaan operaatiot samassa transaktiossa
    Await.result(db.run(
      DBIO.seq(updateOldVersionsAction, insertNewVersionsAction).transactionally
    ), DB_TIMEOUT)
  }

  // Lisätään yliajo jolla ei arvoa
  def poistaYliajo(henkiloOid: String, hakuOid: String, avain: String, virkailijaOid: String, selite: String, poistoHetki: Instant = Instant.now): Unit = {
    tallennaYliajot(Seq(AvainArvoYliajo(avain, None, henkiloOid, hakuOid, virkailijaOid, selite)), poistoHetki)
  }

  def haeYliajoMuutokset(henkiloOid: String, hakuOid: String, avain: String): Seq[AvainArvoYliajoMuutos] = {
    Await.result(db.run(
      sql"""
        SELECT
          arvo,
          to_json(lower(voimassaolo)::timestamptz)#>>'{}' as luotu,
          virkailija_oid,
          selite
        FROM yliajot
        WHERE henkilo_oid = ${henkiloOid}
          AND haku_oid = ${hakuOid}
          AND avain = ${avain}
      """.as[(String, String, String, String)]
        .map(rows => rows.map {
          case (arvo, luotu, virkailijaOid, selite) =>
            AvainArvoYliajoMuutos(Option.apply(arvo), Instant.parse(luotu), virkailijaOid, selite)
        })
    ), DB_TIMEOUT)
      .reverse
  }

  /**
   * Hakee session CAS-tiketin tunnisteella
   */
  def getSessionIdByMappingId(mappingId: String): Option[String] =
    val action =
      sql"""
            SELECT session_id
            FROM cas_client_session
            WHERE mapped_ticket_id = $mappingId
          """.as[String]
    Await.result(db.run(action), DB_TIMEOUT).find(v => true)

  /**
   * Poistaa CAS-sessiomappauksen sessio id:n perusteella
   */
  def deleteCasMappingBySessionId(sessionId: String): Unit =
    val action =
      sqlu"""
            DELETE
            FROM cas_client_session
            WHERE session_id = $sessionId
          """
    Await.result(db.run(action), DB_TIMEOUT)

  /**
   * Lisää kantaan mappauksen palvelun sessiosta CAS-sessioon
   */
  def addMappingForSessionId(mappingId: String, sessionId: String): Unit = {
    val insertAction =
      sqlu"""INSERT INTO cas_client_session (mapped_ticket_id, session_id) VALUES ($mappingId, $sessionId)
             ON CONFLICT (mapped_ticket_id) DO NOTHING"""
    Await.result(db.run(insertAction), DB_TIMEOUT)
  }

  /**
   * Päivittää jobin tilan
   */
  def updateJobStatus(id: UUID, name: String, progress: BigDecimal, udpated: Instant = Instant.now()): Unit = {
    val insertAction =
      sqlu"""INSERT INTO task_status (task_instance, task_name, progress, lastupdated) VALUES (${id.toString}, $name, $progress, ${udpated.toString}::timestamptz)
             ON CONFLICT (task_instance) DO UPDATE SET progress=$progress, lastupdated=now()"""
    Await.result(db.run(insertAction), DB_TIMEOUT)
  }

  /**
   * Hakee viimeisimpien jobien tiedot
   */
  def getLastJobStatuses(name: Option[String], tunniste: Option[UUID], limit: Int): List[Job] = {
    Await.result(db.run(
      sql"""SELECT task_instance, task_name, progress, to_json(lastupdated)#>>'{}'
            FROM task_status
            WHERE (${tunniste.isEmpty} OR task_instance=${tunniste.map(_.toString).getOrElse("NULL")})
            AND (${name.isEmpty} OR task_name=${name.getOrElse("NULL")})
            ORDER BY lastupdated DESC
            LIMIT ${limit}""".as[(String, String, BigDecimal, String)]
        .map(rows => rows.map {
          case (tunniste, nimi, progress, lastUpdated) =>
            Job(UUID.fromString(tunniste), nimi, progress, Instant.parse(lastUpdated))
        })
    ), DB_TIMEOUT).toList
  }

}

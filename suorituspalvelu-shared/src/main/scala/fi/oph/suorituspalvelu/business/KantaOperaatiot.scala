package fi.oph.suorituspalvelu.business

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.oph.suorituspalvelu.business.KantaOperaatiot.{KantaEntiteetit, MAPPER, XMLMAPPER}
import org.skyscreamer.jsonassert.{JSONCompare, JSONCompareMode}
import slick.jdbc.{GetResult, JdbcBackend, SQLActionBuilder, SetParameter}
import slick.jdbc.PostgresProfile.api.*
import com.github.tminglei.slickpg.utils.PlainSQLUtils.mkArraySetParameter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import org.slf4j.LoggerFactory

import java.time.Instant
import java.util.UUID
import KantaOperaatiot.KantaEntiteetit.*
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
      classOf[AmmatillinenPerustutkinto],
      classOf[AmmattiTutkinto],
      classOf[GeneerinenOpiskeluoikeus],
      classOf[YOOpiskeluoikeus],
      classOf[PerusopetuksenVuosiluokka],
      classOf[Telma],
      classOf[PerusopetuksenOppimaaranOppiaineidenSuoritus],
      classOf[Tuva],
      classOf[VirtaTutkinto],
      classOf[Opintosuoritus],
      classOf[VapaaSivistystyo],
      classOf[EBTutkinto],
      classOf[ErikoisAmmattiTutkinto])
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

  def isNewVersion(oppijaNumero: String, suoritusJoukko: SuoritusJoukko, data: Seq[String], fetchedAt: Instant): DBIOAction[Boolean, NoStream, Effect] =
    sql"""
            SELECT to_json(lower(voimassaolo)::timestamptz)#>>'{}' as alku, data_json, data_xml
            FROM versiot
            WHERE oppijanumero=${oppijaNumero} AND suoritusjoukko=${suoritusJoukko.nimi} AND upper(voimassaolo)='infinity'::timestamptz
        """.as[(String, Seq[String], Seq[String])].map(result => {
      if(result.isEmpty)
        true
      else
        val (alku, existingJsonData, existingXmlData) = result.head
        if(fetchedAt.toEpochMilli<=Instant.parse(alku).toEpochMilli)
          LOG.info(s"Ei tarvetta tallentaa uutta versiota oppijalle $oppijaNumero, koska aikaisemmin tallennettu versio on uudempi.")
          false
        else
          suoritusJoukko match
            case SuoritusJoukko.VIRTA =>
              existingXmlData.length != data.length ||
              existingXmlData.sorted.zip(data.sorted).exists((existingDataItem, dataItem) => {
                val existingDataAsJson = MAPPER.writeValueAsString(XMLMAPPER.readValue(existingDataItem, classOf[Map[Any, Any]]))
                val dataAsJson = MAPPER.writeValueAsString(XMLMAPPER.readValue(dataItem, classOf[Map[Any, Any]]))
                if (JSONCompare.compareJSON(existingDataAsJson, dataAsJson, JSONCompareMode.NON_EXTENSIBLE).passed())
                  LOG.info(s"Ei tarvetta tallentaa uutta versiota oppijalle $oppijaNumero, koska haetut tiedot ovat samat kuin kannasta löytyneellä voimassa olevalla versiolla.")
                  false
                else
                  true
              })
            case default =>
              existingJsonData.length != data.length ||
              existingJsonData.sorted.zip(data.sorted).exists((existingDataItem, dataItem) => {
                if (JSONCompare.compareJSON(existingDataItem, dataItem, JSONCompareMode.NON_EXTENSIBLE).passed())
                  LOG.info(s"Ei tarvetta tallentaa uutta versiota oppijalle $oppijaNumero, koska haetut tiedot ovat samat kuin kannasta löytyneellä voimassa olevalla versiolla.")
                  false
                else
                  true
              })
    })

  def tallennaJarjestelmaVersio(oppijaNumero: String, suoritusJoukko: SuoritusJoukko, data: Seq[String], fetchedAt: Instant): Option[VersioEntiteetti] =
    val insertOppijaAction = sqlu"INSERT INTO oppijat(oppijanumero) VALUES (${oppijaNumero}) ON CONFLICT DO NOTHING"
    val lockOppijaAction = sql"""SELECT 1 FROM oppijat WHERE oppijanumero=${oppijaNumero} FOR UPDATE"""
    val insertVersioIfNewDataAction = DBIO.sequence(Seq(insertOppijaAction, lockOppijaAction.as[Int]))
      .flatMap(_ => isNewVersion(oppijaNumero, suoritusJoukko, data, fetchedAt)).flatMap(isNewVersion => {
        if (!isNewVersion)
          DBIO.sequence(Seq.empty).map(_ => None)
        else
          val tunniste = getUUID()
          val timestamp = Instant.ofEpochMilli(fetchedAt.toEpochMilli)
          val discontinueOldVersionAction =
            sql"""
                 UPDATE versiot
                 SET voimassaolo=tstzrange(lower(voimassaolo), ${timestamp.toString}::timestamptz)
                 WHERE oppijanumero=${oppijaNumero} AND suoritusjoukko=${suoritusJoukko.nimi} AND upper(voimassaolo)='infinity'::timestamptz
                 RETURNING tunniste::text""".as[String]
          val insertVersioAction = discontinueOldVersionAction
            .flatMap(useVersioTunnisteet => {
              val useVersioTunniste = if (useVersioTunnisteet.isEmpty) {
                // tilanteessa jossa kyseessä oppijan ensimmäinen versio käytetään edellisenä versiona talletettavaa versiota
                // jotta a) pystytään indikoimaan että versio ei ole käytössä (ts. use_version_tunniste ei null), ja
                // b) foreign key constraint toimii (pitää viitata taulussa olevaan versioon)
                tunniste.toString
              } else useVersioTunnisteet.head
              suoritusJoukko match
                case SuoritusJoukko.VIRTA => sqlu"""
                    INSERT INTO versiot(tunniste, use_versio_tunniste, oppijanumero, voimassaolo, suoritusjoukko, data_xml)
                    VALUES(${tunniste.toString}::uuid, ${useVersioTunniste}::uuid, ${oppijaNumero}, tstzrange(${timestamp.toString}::timestamptz, 'infinity'::timestamptz), ${suoritusJoukko.nimi}, ${data}::xml[])"""
                case default => sqlu"""
                    INSERT INTO versiot(tunniste, use_versio_tunniste, oppijanumero, voimassaolo, suoritusjoukko, data_json)
                    VALUES(${tunniste.toString}::uuid, ${useVersioTunniste}::uuid, ${oppijaNumero}, tstzrange(${timestamp.toString}::timestamptz, 'infinity'::timestamptz), ${suoritusJoukko.nimi}, ${data}::jsonb[])"""
            })
          insertVersioAction.map(_ => Some(VersioEntiteetti(tunniste, oppijaNumero, timestamp, None, suoritusJoukko)))
      })
    Await.result(db.run(insertVersioIfNewDataAction.transactionally), DB_TIMEOUT)

  def haeOppijanVersiot(oppijaNumero: String) = {
    Await.result(db.run(
        sql"""
        SELECT jsonb_build_object(
          'tunniste', versiot.tunniste,
          'oppijaNumero', versiot.oppijanumero,
          'alku',to_json(lower(voimassaolo)::timestamptz)#>>'{}',
          'loppu', CASE WHEN upper(voimassaolo)='infinity'::timestamptz THEN null ELSE to_json(upper(voimassaolo)::timestamptz)#>>'{}' END,
          'suoritusJoukko', versiot.suoritusjoukko
        )::text AS versio
        FROM versiot where oppijanumero = $oppijaNumero""".as[String]), DB_TIMEOUT)
      .map(json => MAPPER.readValue(json, classOf[VersioEntiteetti]))
  }

  def haeUusimmatMuuttuneetVersiot(alkaen: Instant): Seq[VersioEntiteetti] =
    Await.result(db.run(
      sql"""
        WITH RECURSIVE
          w_versiot_in_use(tunniste, use_versio_tunniste, loppu) AS (
              SELECT versiot.tunniste, versiot.use_versio_tunniste, upper(versiot.voimassaolo)
              FROM versiot
              WHERE use_versio_tunniste IS NOT NULL
                  OR (use_versio_tunniste IS NULL AND lower(voimassaolo)>=${Instant.ofEpochMilli(alkaen.toEpochMilli).toString}::timestamptz)
            UNION
              SELECT versiot.tunniste, versiot.use_versio_tunniste, upper(versiot.voimassaolo)
              FROM w_versiot_in_use JOIN versiot ON w_versiot_in_use.use_versio_tunniste=versiot.tunniste
              WHERE w_versiot_in_use.loppu>upper(versiot.voimassaolo) -- estetään syklit
              AND w_versiot_in_use.tunniste<>versiot.tunniste -- estetään syklit
              AND lower(voimassaolo)>=${Instant.ofEpochMilli(alkaen.toEpochMilli).toString}::timestamptz
          ),
          w_versiot AS (
            SELECT jsonb_build_object(
              'tunniste', versiot.tunniste,
              'oppijaNumero', versiot.oppijanumero,
              'alku',to_json(lower(versiot.voimassaolo)::timestamptz)#>>'{}',
              'loppu', CASE WHEN loppu='infinity'::timestamptz THEN null ELSE to_json(loppu::timestamptz)#>>'{}' END,
              'suoritusJoukko', versiot.suoritusjoukko
            )::text AS versio
            FROM w_versiot_in_use JOIN versiot ON w_versiot_in_use.tunniste=versiot.tunniste
            WHERE w_versiot_in_use.use_versio_tunniste IS NULL
          )
          SELECT * FROM w_versiot""".as[String]), DB_TIMEOUT)
      .map(json => MAPPER.readValue(json, classOf[VersioEntiteetti]))

  def haeData(versio: VersioEntiteetti): (VersioEntiteetti, Seq[String]) =
    Await.result(db.run(
      sql"""SELECT jsonb_build_object('tunniste', tunniste,
              'oppijaNumero', oppijanumero,
              'alku',to_json(lower(voimassaolo)::timestamptz)#>>'{}',
              'loppu', CASE WHEN upper(voimassaolo)='infinity'::timestamptz THEN null ELSE to_json(upper(voimassaolo)::timestamptz)#>>'{}' END,
              'suoritusJoukko', suoritusjoukko
            )::text AS versio,
            CASE WHEN suoritusjoukko='VIRTA' THEN data_xml::text[] ELSE data_json::text[] END
            FROM versiot
            WHERE tunniste=${versio.tunniste.toString}::UUID""".as[(String, Seq[String])]), DB_TIMEOUT)
      .map((json, data) => (MAPPER.readValue(json, classOf[VersioEntiteetti]), data)).head

  def tallennaVersioonLiittyvatEntiteetit(versio: VersioEntiteetti, opiskeluoikeudet: Set[Opiskeluoikeus], metadata: Map[String, Set[String]] = Map.empty) = {
    LOG.info(s"Tallennetaan versioon $versio liittyvät opiskeluoikeudet (${opiskeluoikeudet.size}) kpl")
    val deletePrevious = sqlu"""DELETE FROM opiskeluoikeudet WHERE versio_tunniste=${versio.tunniste.toString}::uuid"""
    val updateVersion = sqlu"""UPDATE versiot SET use_versio_tunniste=NULL, metadata=${metadata.map((avain, arvot) => arvot.map(arvo => avain + ":" + arvo)).flatten.toSeq} WHERE tunniste=${versio.tunniste.toString}::uuid"""
    val dataInserts = opiskeluoikeudet.map(opiskeluoikeus =>
      sqlu"""
            INSERT INTO opiskeluoikeudet (versio_tunniste, data_parseroitu) VALUES(${versio.tunniste.toString}::uuid, ${MAPPER.writeValueAsString(Container(opiskeluoikeus))}::jsonb)
            """)
    val metadataArvotInserts = metadata.flatMap((avain, arvot) => arvot.map(arvo => sqlu"""INSERT INTO metadata_arvot (avain, arvo) VALUES($avain, $arvo) ON CONFLICT DO NOTHING"""))
    Await.result(db.run(DBIO.sequence(Seq(deletePrevious) ++ dataInserts ++ Seq(updateVersion) ++ metadataArvotInserts).transactionally), DB_TIMEOUT)
  }

  private def haeSuorituksetInternal(versioTunnisteetQuery: slick.jdbc.SQLActionBuilder): Map[VersioEntiteetti, Set[Opiskeluoikeus]] = {
    var versiotByKey = Map.empty[String, VersioEntiteetti]
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
                AND w_versiot_in_use.tunniste<>versiot.tunniste
            ),
            w_versiot AS (
              SELECT versiot.tunniste AS versio_tunniste,
                jsonb_build_object(
                  'tunniste', versiot.tunniste,
                  'oppijaNumero', versiot.oppijanumero,
                  'alku',to_json(lower(versiot.voimassaolo)::timestamptz)#>>'{}',
                  'loppu', CASE WHEN loppu='infinity'::timestamptz THEN null ELSE to_json(loppu::timestamptz)#>>'{}' END,
                  'suoritusJoukko', versiot.suoritusjoukko
                )::text AS data
              FROM w_versiot_in_use JOIN versiot ON w_versiot_in_use.tunniste=versiot.tunniste
              WHERE w_versiot_in_use.use_versio_tunniste IS NULL
            ),
            w_data AS(
              SELECT opiskeluoikeudet.versio_tunniste AS versio_tunniste, data_parseroitu::text AS data
              FROM opiskeluoikeudet JOIN w_versiot ON opiskeluoikeudet.versio_tunniste=w_versiot.versio_tunniste
            )
          SELECT 1 as priority, 'versio' AS tyyppi, null AS versio_tunniste, data FROM w_versiot
          UNION ALL
          SELECT 2 as priority, 'opiskeluoikeus' as tyyppi, versio_tunniste, data FROM w_data
          ORDER BY priority ASC;
        """).as[(Int, String, String, String)]), DB_TIMEOUT).flatMap((priority, tyyppi, versioTunniste, data) => {
        tyyppi match {
          case "versio" =>
            val versio = MAPPER.readValue(data, classOf[VersioEntiteetti])
            versiotByKey = versiotByKey + (versio.tunniste.toString -> versio)
            None
          case "opiskeluoikeus" =>
            val container = MAPPER.readValue(data, classOf[Container])
            Some(versiotByKey(versioTunniste), container.opiskeluoikeus)
        }
      })
      .groupBy((versio, _) => versio)
      .map((versio, suoritukset) => (versio, suoritukset.map(t => t._2).toSet))
  }

  def haeSuoritukset(oppijaNumero: String): Map[VersioEntiteetti, Set[Opiskeluoikeus]] =
    haeSuorituksetInternal(sql"""SELECT tunniste FROM versiot WHERE oppijanumero=${oppijaNumero} AND upper(voimassaolo)='infinity'::timestamptz""")

  def haeVersio(tunniste: UUID): Option[VersioEntiteetti] =
    Await.result(db.run(
        sql"""SELECT jsonb_build_object(
                'tunniste', tunniste,
                'oppijaNumero', oppijanumero,
                'alku',to_json(lower(voimassaolo)::timestamptz)#>>'{}',
                'loppu', CASE WHEN upper(voimassaolo)='infinity'::timestamptz THEN null ELSE to_json(upper(voimassaolo)::timestamptz)#>>'{}' END,
                'suoritusJoukko', suoritusjoukko
              )::text AS versio
              FROM versiot
              WHERE tunniste=${tunniste.toString}::UUID""".as[String]), DB_TIMEOUT)
      .map(json => MAPPER.readValue(json, classOf[VersioEntiteetti])).headOption

  def parseMetadata(avainArvot: Seq[String]): Map[String, Set[String]] =
    avainArvot.map(element => {
        val key = element.split(":")(0)
        val value = element.substring(key.length + ":".length)
        key -> value
      })
      .groupBy((key, value) => key)
      .map((key, values) => key -> values.map(v => v._2).toSet)

  def haeVersiotJaMetadata(metadata: Map[String, Set[String]], timestamp: Instant): Seq[(VersioEntiteetti, Map[String, Set[String]])] =
    Await.result(db.run(
        (sql"""
          SELECT
            jsonb_build_object(
              'tunniste', versiot.tunniste,
              'oppijaNumero', versiot.oppijanumero,
              'alku',to_json(lower(versiot.voimassaolo)::timestamptz)#>>'{}',
              'loppu', CASE WHEN upper(voimassaolo)='infinity'::timestamptz THEN null ELSE to_json(upper(voimassaolo)::timestamptz)#>>'{}' END,
              'suoritusJoukko', versiot.suoritusjoukko
            )::text AS data,
            metadata
          FROM versiot
          WHERE versiot.use_versio_tunniste IS NULL
          AND metadata @> (${metadata.flatMap((avain, arvot) => arvot.map(arvo => avain + ":" + arvo)).toSeq})
          AND ${Instant.ofEpochMilli(timestamp.toEpochMilli).toString}::timestamptz <@ voimassaolo
        """).as[(String, List[String])]), DB_TIMEOUT)
      .map((data, metadata) => (MAPPER.readValue(data, classOf[VersioEntiteetti]), parseMetadata(metadata)))

  def paataVersionVoimassaolo(tunniste: UUID): Boolean =
    LOG.info(s"päätetään version $tunniste voimassaolo")
    val voimassaolo = sqlu"""UPDATE versiot SET voimassaolo=tstzrange(lower(voimassaolo), now()) WHERE tunniste=${tunniste.toString}::uuid AND upper(voimassaolo)='infinity'::timestamptz"""
    Await.result(db.run(voimassaolo), DB_TIMEOUT)>0

  def haeMetadataAvaimenArvot(avain: String, prefix: Option[String] = None): Set[String] =
    prefix match
      case None => Await.result(db.run(sql"""SELECT arvo FROM metadata_arvot WHERE avain=$avain""".as[String]), DB_TIMEOUT).toSet
      case Some(prefix) => Await.result(db.run(sql"""SELECT arvo FROM metadata_arvot WHERE avain=$avain AND arvo LIKE ${s"$prefix%"}""".as[String]), DB_TIMEOUT).toSet

  def haeOppijanYliajot(oppijaNumero: String, hakuOid: String): Seq[AvainArvoYliajo] = {
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
        WHERE henkilo_oid = ${oppijaNumero}
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

  def haeYliajoMuutokset(oppijaNumero: String, hakuOid: String, avain: String): Seq[AvainArvoYliajoMuutos] = {
    Await.result(db.run(
      sql"""
        SELECT
          arvo,
          to_json(lower(voimassaolo)::timestamptz)#>>'{}' as luotu,
          virkailija_oid,
          selite
        FROM yliajot
        WHERE henkilo_oid = ${oppijaNumero}
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

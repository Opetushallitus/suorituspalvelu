package fi.oph.suorituspalvelu.business

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.oph.suorituspalvelu.business.KantaOperaatiot.{KantaEntiteetit, MAPPER}
import org.skyscreamer.jsonassert.{JSONCompare, JSONCompareMode}
import slick.jdbc.{JdbcBackend, SQLActionBuilder, SetParameter}
import slick.jdbc.PostgresProfile.api.*
import com.github.tminglei.slickpg.utils.PlainSQLUtils.mkArraySetParameter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import org.slf4j.LoggerFactory

import java.time.Instant
import java.util.UUID
import KantaOperaatiot.KantaEntiteetit.*

implicit val setStringArray: SetParameter[Seq[String]] = mkArraySetParameter[String]("varchar")

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
      classOf[NuortenPerusopetuksenOppiaineenOppimaara],
      classOf[Tuva],
      classOf[VirtaTutkinto],
      classOf[Opintosuoritus],
      classOf[VapaaSivistystyo])
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

  def isNewVersion(oppijaNumero: String, suoritusJoukko: SuoritusJoukko, data: String, fetchedAt: Instant): DBIOAction[Boolean, NoStream, Effect] =
    sql"""
            SELECT to_json(lower(voimassaolo)::timestamptz)#>>'{}' as alku, data_json
            FROM versiot
            WHERE oppijanumero=${oppijaNumero} AND suoritusjoukko=${suoritusJoukko.nimi} AND upper(voimassaolo)='infinity'::timestamptz
        """.as[(String, String)].map(result => {
      if(result.isEmpty)
        true
      else
        val (alku, existingData) = result.head
        if(fetchedAt.toEpochMilli<=Instant.parse(alku).toEpochMilli)
          LOG.info(s"Ei tarvetta tallentaa uutta versiota oppijalle $oppijaNumero, koska aikaisemmin tallennettu versio on uudempi.")
          false
        else
          suoritusJoukko match
            case SuoritusJoukko.VIRTA => true // TODO: ei toteutettu
            case default =>
              if(JSONCompare.compareJSON(existingData, data, JSONCompareMode.NON_EXTENSIBLE).passed())
                LOG.info(s"Ei tarvetta tallentaa uutta versiota oppijalle $oppijaNumero, koska haetut tiedot ovat samat kuin kannasta löytyneellä voimassa olevalla versiolla.")
                false
              else
                true
    })

  def tallennaJarjestelmaVersio(oppijaNumero: String, suoritusJoukko: SuoritusJoukko, data: String, fetchedAt: Instant): Option[VersioEntiteetti] =
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
                    VALUES(${tunniste.toString}::uuid, ${useVersioTunniste}::uuid, ${oppijaNumero}, tstzrange(${timestamp.toString}::timestamptz, 'infinity'::timestamptz), ${suoritusJoukko.nimi}, ${data}::xml)"""
                case default => sqlu"""
                    INSERT INTO versiot(tunniste, use_versio_tunniste, oppijanumero, voimassaolo, suoritusjoukko, data_json)
                    VALUES(${tunniste.toString}::uuid, ${useVersioTunniste}::uuid, ${oppijaNumero}, tstzrange(${timestamp.toString}::timestamptz, 'infinity'::timestamptz), ${suoritusJoukko.nimi}, ${data}::jsonb)"""
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

  def haeData(versio: VersioEntiteetti): (VersioEntiteetti, String) =
    Await.result(db.run(
      sql"""SELECT jsonb_build_object('tunniste', tunniste,
              'oppijaNumero', oppijanumero,
              'alku',to_json(lower(voimassaolo)::timestamptz)#>>'{}',
              'loppu', CASE WHEN upper(voimassaolo)='infinity'::timestamptz THEN null ELSE to_json(upper(voimassaolo)::timestamptz)#>>'{}' END,
              'suoritusJoukko', suoritusjoukko
            )::text AS versio,
            CASE WHEN suoritusjoukko='VIRTA' THEN data_xml::text ELSE data_json::text END
            FROM versiot
            WHERE tunniste=${versio.tunniste.toString}::UUID""".as[(String, String)]), DB_TIMEOUT)
      .map((json, data) => (MAPPER.readValue(json, classOf[VersioEntiteetti]), data)).head

  def tallennaVersioonLiittyvatEntiteetit(versio: VersioEntiteetti, opiskeluoikeudet: Set[Opiskeluoikeus], metadata: Map[String, Set[String]] = Map.empty) = {
    LOG.info(s"Tallennetaan versioon $versio liittyvät opiskeluoikeudet (${opiskeluoikeudet.size}) kpl")
    val deletePrevious = sqlu"""DELETE FROM opiskeluoikeudet WHERE versio_tunniste=${versio.tunniste.toString}::uuid"""
    val enableVersion = sqlu"""UPDATE versiot SET use_versio_tunniste=NULL WHERE tunniste=${versio.tunniste.toString}::uuid"""
    val dataInserts = opiskeluoikeudet.map(opiskeluoikeus =>
      sqlu"""
            INSERT INTO opiskeluoikeudet (versio_tunniste, data_parseroitu, metadata) VALUES(${versio.tunniste.toString}::uuid, ${MAPPER.writeValueAsString(Container(opiskeluoikeus))}::jsonb, ${metadata.map((avain, arvot) => arvot.map(arvo => avain + ":" + arvo)).flatten.toSeq})
            """)
    Await.result(db.run(DBIO.sequence(Seq(deletePrevious) ++ dataInserts ++ Seq(enableVersion))), DB_TIMEOUT)
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
        sql"""SELECT jsonb_build_object('tunniste', tunniste,
                  'oppijaNumero', oppijanumero,
                  'alku',to_json(lower(voimassaolo)::timestamptz)#>>'{}',
                  'loppu', CASE WHEN upper(voimassaolo)='infinity'::timestamptz THEN null ELSE to_json(upper(voimassaolo)::timestamptz)#>>'{}' END,
                  'suoritusJoukko', suoritusjoukko
                )::text AS versio
                FROM versiot
                WHERE tunniste=${tunniste.toString}::UUID""".as[String]), DB_TIMEOUT)
      .map(json => MAPPER.readValue(json, classOf[VersioEntiteetti])).headOption

  def haeVersiot(metadata: Map[String, Set[String]]): Set[VersioEntiteetti] =
    Await.result(db.run(
        (sql"""
          WITH RECURSIVE
            w_versio_tunnisteet AS (
              SELECT versio_tunniste FROM opiskeluoikeudet WHERE metadata @> (${metadata.flatMap((avain, arvot) => arvot.map(arvo => avain + ":" + arvo)).toSeq})
            ),
            w_versiot AS (
              SELECT versiot.tunniste AS versio_tunniste,
                jsonb_build_object(
                  'tunniste', versiot.tunniste,
                  'oppijaNumero', versiot.oppijanumero,
                  'alku',to_json(lower(versiot.voimassaolo)::timestamptz)#>>'{}',
                  'loppu', CASE WHEN upper(voimassaolo)='infinity'::timestamptz THEN null ELSE to_json(upper(voimassaolo)::timestamptz)#>>'{}' END,
                  'suoritusJoukko', versiot.suoritusjoukko
                )::text AS data
              FROM w_versio_tunnisteet JOIN versiot ON w_versio_tunnisteet.versio_tunniste=versiot.tunniste
              WHERE versiot.use_versio_tunniste IS NULL
            )
          SELECT data FROM w_versiot;
        """).as[String]), DB_TIMEOUT).map(data => MAPPER.readValue(data, classOf[VersioEntiteetti]))
      .toSet

  def paataVersionVoimassaolo(tunniste: UUID): Boolean =
    LOG.info(s"päätetään version $tunniste voimassaolo")
    val voimassaolo = sqlu"""UPDATE versiot SET voimassaolo=tstzrange(lower(voimassaolo), now()) WHERE tunniste=${tunniste.toString}::uuid AND upper(voimassaolo)='infinity'::timestamptz"""
    Await.result(db.run(voimassaolo), DB_TIMEOUT)>0
}

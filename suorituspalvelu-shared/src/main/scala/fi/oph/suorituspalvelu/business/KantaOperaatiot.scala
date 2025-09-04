package fi.oph.suorituspalvelu.business

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.oph.suorituspalvelu.business.KantaOperaatiot.{KantaEntiteetit, MAPPER}
import org.skyscreamer.jsonassert.{JSONCompare, JSONCompareMode}
import slick.jdbc.{JdbcBackend, SQLActionBuilder}
import slick.jdbc.PostgresProfile.api.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import org.slf4j.LoggerFactory

import java.time.Instant
import java.util.UUID
import KantaOperaatiot.KantaEntiteetit.*

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
      classOf[Opintosuoritus])
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

  def sameAsExistingData(oppijaNumero: String, suoritusJoukko: SuoritusJoukko, data: String): DBIOAction[Boolean, NoStream, Effect] =
    suoritusJoukko match
      case SuoritusJoukko.VIRTA => sql"""SELECT 1""".as[String].map(_ => false) // TODO: ei toteutettu
      case default =>
        sql"""
            SELECT data_json
            FROM versiot
            WHERE oppijanumero=${oppijaNumero} AND suoritusjoukko=${suoritusJoukko.nimi} AND upper(voimassaolo)='infinity'::timestamptz
        """.as[String].map(existingData => {
            if(existingData.isEmpty)
              false
            else
              JSONCompare.compareJSON(existingData.head, data, JSONCompareMode.NON_EXTENSIBLE).passed()
          })

  def tallennaJarjestelmaVersio(oppijaNumero: String, suoritusJoukko: SuoritusJoukko, data: String): Option[VersioEntiteetti] =
    val insertOppijaAction = sqlu"INSERT INTO oppijat(oppijanumero) VALUES (${oppijaNumero}) ON CONFLICT DO NOTHING"
    val lockOppijaAction = sql"""SELECT 1 FROM oppijat WHERE oppijanumero=${oppijaNumero} FOR UPDATE"""
    val insertVersioIfNewDataAction = DBIO.sequence(Seq(insertOppijaAction, lockOppijaAction.as[Int]))
      .flatMap(_ => sameAsExistingData(oppijaNumero, suoritusJoukko, data)).flatMap(isSame => {
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
          'tietolahde', versiot.lahde
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
              OR (use_versio_tunniste IS NULL AND lower(voimassaolo)>=${alkaen.toString}::timestamptz)
            UNION
              SELECT versiot.tunniste, versiot.use_versio_tunniste, upper(versiot.voimassaolo)
              FROM w_versiot_in_use JOIN versiot ON w_versiot_in_use.use_versio_tunniste=versiot.tunniste
              WHERE w_versiot_in_use.loppu>upper(versiot.voimassaolo) -- estetään syklit
              AND w_versiot_in_use.tunniste<>versiot.tunniste -- estetään syklit
              AND lower(voimassaolo)>=${alkaen.toString}::timestamptz
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

  def tallennaVersioonLiittyvatEntiteetit(versio: VersioEntiteetti, opiskeluoikeudet: Set[Opiskeluoikeus], suoritukset: Set[Suoritus]) = {
    LOG.info(s"Tallennetaan versioon $versio liittyvät opiskeluoikeudet (${opiskeluoikeudet.size}) ja suoritukset (${suoritukset.size})")
    val dataInsert = sqlu"""UPDATE versiot SET use_versio_tunniste=NULL, data_parseroitu=${MAPPER.writeValueAsString(Container(opiskeluoikeudet))}::jsonb WHERE tunniste=${versio.tunniste.toString}::uuid"""
    Await.result(db.run(dataInsert), DB_TIMEOUT)
  }

  private def haeSuorituksetInternal(versioTunnisteetQuery: slick.jdbc.SQLActionBuilder): Map[VersioEntiteetti, Set[Opiskeluoikeus]] =
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
              SELECT jsonb_build_object(
                'tunniste', versiot.tunniste,
                'oppijaNumero', versiot.oppijanumero,
                'alku',to_json(lower(versiot.voimassaolo)::timestamptz)#>>'{}',
                'loppu', CASE WHEN loppu='infinity'::timestamptz THEN null ELSE to_json(loppu::timestamptz)#>>'{}' END,
                'suoritusJoukko', versiot.suoritusjoukko
              )::text AS versio,
              data_parseroitu::text AS data
              FROM w_versiot_in_use JOIN versiot ON w_versiot_in_use.tunniste=versiot.tunniste
              WHERE w_versiot_in_use.use_versio_tunniste IS NULL
            )
          SELECT * FROM w_versiot;
        """).as[(String, String)]), DB_TIMEOUT).map((versioData, data) =>
        val versio = MAPPER.readValue(versioData, classOf[VersioEntiteetti])
        val container = MAPPER.readValue(data, classOf[Container])
        (versio, container.opiskeluoikeudet))
      .groupBy((versio, _) => versio)
      .map((versio, suoritukset) => (versio, suoritukset.map(t => t._2).flatten.toSet))

  def haeSuoritukset(oppijaNumero: String): Map[VersioEntiteetti, Set[Opiskeluoikeus]] =
    haeSuorituksetInternal(sql"""SELECT tunniste FROM versiot WHERE oppijanumero=${oppijaNumero} AND upper(voimassaolo)='infinity'::timestamptz""")
}

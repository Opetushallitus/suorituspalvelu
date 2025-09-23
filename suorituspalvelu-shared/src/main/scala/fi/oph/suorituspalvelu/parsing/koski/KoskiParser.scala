package fi.oph.suorituspalvelu.parsing.koski

import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import java.io.InputStream
import java.time.LocalDate

trait VersioituTunniste {
  def koodiarvo: String
  def koodistoUri: String
  def koodistoVersio: Option[Int]
}

case class Kielistetty(fi: Option[String],
                       sv: Option[String],
                       en: Option[String]) {
  def pickWithFallback() = {
    (fi, sv, en) match {
      case (Some(fi), _, _) => fi
      case (_, Some(sv), _) => sv
      case (_, _, Some(en)) => en
      case _ => ""
    }
  }
}

case class KoskiKoodi(koodiarvo: String,
                      koodistoUri: String,
                      nimi: Kielistetty,
                      koodistoVersio: Option[Int]) extends VersioituTunniste

case class Arviointi(arvosana: KoskiKoodi,
                     `päivä`: Option[String],
                     hyväksytty: Boolean)

case class Laajuus(arvo: BigDecimal,
                   yksikkö: Option[Yksikko])

case class OsaSuoritus(tyyppi: SuoritusTyyppi,
                       koulutusmoduuli: Option[KoulutusModuuli],
                       arviointi: Option[Set[Arviointi]],
                       `yksilöllistettyOppimäärä`: Option[Boolean],
                       `rajattuOppimäärä`: Option[Boolean],
                       osasuoritukset: Option[Set[OsaSuoritus]])

case class SuoritusKieli(koodiarvo: String,
                         koodistoUri: String,
                         koodistoVersio: Option[Int]) extends VersioituTunniste

case class SuoritusTapa(koodiarvo: String,
                        koodistoUri: String,
                        koodistoVersio: Option[Int],
                        nimi: Kielistetty) extends VersioituTunniste

case class Yksikko(koodiarvo: String,
                   koodistoUri: String,
                   koodistoVersio: Option[Int],
                   nimi: Kielistetty,
                   lyhytNimi: Kielistetty) extends VersioituTunniste

case class KoulutusModuuli(tunniste: Option[KoskiKoodi],
                           koulutustyyppi: Option[KoskiKoodi],
                           laajuus: Option[Laajuus],
                           kieli: Option[KoskiKoodi],
                           pakollinen: Option[Boolean])

case class SuoritusTyyppi(koodiarvo: String,
                          koodistoUri: String,
                          nimi: Kielistetty)

case class Vahvistus(`päivä`: String)

case class Suoritus(tyyppi: SuoritusTyyppi,
                    koulutusmoduuli: Option[KoulutusModuuli],
                    suorituskieli: Option[SuoritusKieli],
                    koulusivistyskieli: Option[Set[SuoritusKieli]],
                    alkamispäivä: Option[String],
                    vahvistus: Option[Vahvistus],
                    osasuoritukset: Option[Set[OsaSuoritus]],
                    arviointi: Option[Set[Arviointi]],
                    keskiarvo: Option[BigDecimal],
                    suoritustapa: Option[SuoritusTapa],
                    `jääLuokalle`: Option[Boolean])

case class OpiskeluoikeusJaksoTila(koodiarvo: String,
                                   koodistoUri: String,
                                   koodistoVersio: Option[Int]) extends VersioituTunniste

case class OpiskeluoikeusJakso(alku: LocalDate,
                               tila: OpiskeluoikeusJaksoTila)

case class OpiskeluoikeusTila(opiskeluoikeusjaksot: List[OpiskeluoikeusJakso])

case class OpiskeluoikeusTyyppi(koodiarvo: String,
                                koodistoUri: String,
                                koodistoVersio: Option[Int]) extends VersioituTunniste

case class Oppilaitos(nimi: Kielistetty,
                      oid: String)

case class KoskiErityisenTuenPaatos(opiskeleeToimintaAlueittain: Option[Boolean])

case class Kotiopetusjakso(alku: String,
                           loppu: Option[String])

case class KoskiLisatiedot(erityisenTuenPäätökset: Option[List[KoskiErityisenTuenPaatos]],
                           vuosiluokkiinSitoutumatonOpetus: Option[Boolean],
                           kotiopetusjaksot: Option[List[Kotiopetusjakso]])

case class Opiskeluoikeus(oid: String,
                          oppilaitos: Option[Oppilaitos],
                          tyyppi: OpiskeluoikeusTyyppi,
                          tila: Option[OpiskeluoikeusTila],
                          suoritukset: Set[Suoritus],
                          lisätiedot: Option[KoskiLisatiedot]) {
  def isPerusopetus: Boolean = tyyppi.koodiarvo == "perusopetus" || tyyppi.koodiarvo == "aikuistenperusopetus"
  def isAmmatillinen: Boolean = tyyppi.koodiarvo == "ammatillinenkoulutus"
}

case class SplitattavaKoskiData(oppijaOid: String,
                                opiskeluoikeudet: Seq[Map[String, Any]])

/**
 * Parseroi Kosken JSON-muotoisen opiskeluoikeus-suoritusdatan KOSKI-spesifiksi objektipuuksi
 */
object KoskiParser {
  val MAPPER: ObjectMapper = {
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper.registerModule(new JavaTimeModule())
    mapper.registerModule(new Jdk8Module()) // tämä on java.util.Optional -kenttiä varten
    // halutaan mahdollisimman tiukka validaatio ettei sanitointi suodata mitään dataa pois
    mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    mapper.configure(SerializationFeature.INDENT_OUTPUT, true)
    mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
    mapper
  }

  def parseKoskiData(data: String): Seq[Opiskeluoikeus] =
    MAPPER.readValue(data, classOf[Array[Opiskeluoikeus]]).toSeq

  def splitKoskiDataByOppija(input: InputStream): Iterator[(String, String)] =
    val jsonParser = MAPPER.getFactory().createParser(input)
    jsonParser.nextToken()

    Iterator.continually({
      val token = jsonParser.nextToken()
      if(token != JsonToken.END_ARRAY)
        Some(jsonParser.readValueAs(classOf[SplitattavaKoskiData]))
      else
        None})
      .takeWhile(data => data.isDefined)
      .map(data => {
        (data.get.oppijaOid, MAPPER.writeValueAsString(data.get.opiskeluoikeudet))
      })
}

package fi.oph.suorituspalvelu.parsing.virta

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import java.io.{ByteArrayInputStream, InputStream}
import java.time.LocalDate

case class Header()

case class VirtaTila(AlkuPvm: LocalDate, LoppuPvm: LocalDate, Koodi: String)

@JsonDeserialize(classOf[KoulutusalaDeserializer])
case class VirtaOpiskeluoikeusKoulutusala(versio: String, koodi: Int)

case class VirtaLaajuus(Opintopiste: BigDecimal)

case class VirtaJakso(
  @JacksonXmlElementWrapper(useWrapping = false) Nimi: Seq[VirtaNimi],
  Rahoituslahde: Option[String],
  Koulutuskieli: Option[String],
  LoppuPvm: LocalDate,
  AlkuPvm: LocalDate,
  Koulutuskoodi: Option[String],
  koulutusmoduulitunniste: String,
  Koulutuskunta: Option[String]
)

case class VirtaOpiskeluoikeus(
  Laajuus: VirtaLaajuus,
  LoppuPvm: LocalDate,
  koulutusmoduulitunniste: String = "",
  // Tiloja on aina vähintään yksi
  @JacksonXmlElementWrapper(useWrapping = false) Tila: Seq[VirtaTila],
  // Jaksoja voi olla nolla
  @JacksonXmlElementWrapper(useWrapping = false) Jakso: Seq[VirtaJakso],
  Koulutusala: VirtaOpiskeluoikeusKoulutusala,
  Tyyppi: String,
  AlkuPvm: LocalDate,
  Myontaja: String,
  opiskelijaAvain: String,
  avain: String
)

case class VirtaLukukausiIlmoittautuminen(
  IlmoittautumisPvm: LocalDate,
  opiskelijaAvain: String,
  LoppuPvm: LocalDate,
  Tila: String,
  opiskeluoikeusAvain: String,
  AlkuPvm: LocalDate,
  Myontaja: String
)

case class VirtaOrganisaatio(Rooli: String, Koodi: String, Osuus: Option[BigDecimal])

case class VirtaMuuAsteikkoArvosana(avain: String, Koodi: String, Nimi: String)

@JsonDeserialize(classOf[ArvosanaDeserializer])
case class VirtaArvosana(arvosana: String, asteikko: String)

@JsonDeserialize(classOf[KoodiDeserializer])
case class VirtaKoodi(versio: String, koodi: Int)

case class VirtaSuoritusKoulutusala(Koodi: VirtaKoodi)

@JsonDeserialize(classOf[NimiDeserializer])
case class VirtaNimi(kieli: Option[String], nimi: String)

case class VirtaSuoritusviite(sisaltyvaOpintosuoritusAvain: String)

case class VirtaOpintosuoritus(
  Kieli: String,
  Organisaatio: Option[VirtaOrganisaatio],
  Tyyppi: String,
  SuoritusPvm: LocalDate,
  Arvosana: Option[VirtaArvosana],
  opiskeluoikeusAvain: Option[String], // puuttuu osasuorituksilta
  Koulutusala: Option[VirtaSuoritusKoulutusala],
  Laji: Int,
  koulutusmoduulitunniste: String,
  Opinnaytetyo: Option[String],
  Laajuus: VirtaLaajuus,
  HyvaksilukuPvm: Option[LocalDate],
  opiskelijaAvain: String,
  avain: String,
  Myontaja: String,
  Koulutuskoodi: Option[String],
  @JacksonXmlElementWrapper(useWrapping = false) Nimi: Seq[VirtaNimi],
  TKILaajuusHarjoittelu: Option[VirtaLaajuus],
  @JacksonXmlElementWrapper(useWrapping = false) Sisaltyvyys: Seq[VirtaSuoritusviite]
)

case class VirtaOpiskelija(
  Opiskeluoikeudet: Seq[VirtaOpiskeluoikeus],
  LukukausiIlmoittautumiset: Seq[VirtaLukukausiIlmoittautuminen],
  Opintosuoritukset: Option[Seq[VirtaOpintosuoritus]],
  Henkilotunnus: String,
  avain: String
)

case class VirtaOpiskelijanKaikkiTiedot(Virta: Seq[VirtaOpiskelija])

case class Body(OpiskelijanKaikkiTiedotResponse: VirtaOpiskelijanKaikkiTiedot)

case class VirtaSuoritukset(Header: Header, Body: Body)

class KoulutusalaDeserializer extends JsonDeserializer[VirtaOpiskeluoikeusKoulutusala] {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): VirtaOpiskeluoikeusKoulutusala =
    val mixin = p.readValueAs(classOf[KoulutusAlaTaiKoodiMixIn])
    VirtaOpiskeluoikeusKoulutusala(mixin.versio, mixin.value)
}

class KoodiDeserializer extends JsonDeserializer[VirtaKoodi] {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): VirtaKoodi =
    val mixin = p.readValueAs(classOf[KoulutusAlaTaiKoodiMixIn])
    VirtaKoodi(mixin.versio, mixin.value)
}

class NimiDeserializer extends JsonDeserializer[VirtaNimi] {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): VirtaNimi =
    val value = p.readValueAs(classOf[Any])
    value match
      // jos tullaan tähän haaraan, nimiä on vain yksi ja sillä ei ole kieliattribuuttia
      case nimi: String => VirtaNimi(None, nimi)
      // jos tullaan tähän haaraan kieliä on useita ja niillä pitäisi olla kieliattribuutit, jos ei ole niin räjähdetään
      case nimi: Map[_, _] =>
        val nimiMap = nimi.asInstanceOf[Map[String, String]]
        VirtaNimi(Some(nimiMap("kieli")), nimiMap(""))
}

class ArvosanaDeserializer extends JsonDeserializer[VirtaArvosana] {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): VirtaArvosana =
    val (arvosanaTagName, arvosanaContent) = p.readValueAs(classOf[Map[String, Any]]).head
    arvosanaTagName match {
      case "EiKaytossa" => null
      case "Muu" =>
        val mapper = p.getCodec.asInstanceOf[ObjectMapper]
        val contentMap = arvosanaContent.asInstanceOf[Map[String, Any]]
        val asteikkoMap = contentMap("Asteikko").asInstanceOf[Map[String, Any]]
        val koodi = contentMap("Koodi").asInstanceOf[String]
        val asteikkoNimi = asteikkoMap("Nimi").asInstanceOf[String]

        val asteikkoArvosanat = asteikkoMap("AsteikkoArvosana") match {
          case list: java.util.ArrayList[_] =>
            list.toArray.map(mapper.convertValue(_, classOf[VirtaMuuAsteikkoArvosana])).toSeq
          case single: Map[_, _] =>
            Seq(mapper.convertValue(single, classOf[VirtaMuuAsteikkoArvosana]))
        }

        val matchingArvosana = asteikkoArvosanat.find(_.avain == koodi).map(_.Nimi)
        matchingArvosana match {
          case Some(arvosanaNimi) => VirtaArvosana(arvosana = arvosanaNimi, asteikko = asteikkoNimi)
          case None => null
        }
      case _ => VirtaArvosana(arvosana = arvosanaContent.asInstanceOf[String], asteikko = arvosanaTagName)
    }
}

object VirtaParser {
  val MAPPER: ObjectMapper = {
    val mapper = new XmlMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper.registerModule(new JavaTimeModule())
    mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    mapper.configure(SerializationFeature.INDENT_OUTPUT, true)
    mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
    mapper
  }

  def parseVirtaData(suoritukset: String): VirtaSuoritukset =
    parseVirtaData(new ByteArrayInputStream(suoritukset.getBytes))

  def parseVirtaData(suoritukset: InputStream): VirtaSuoritukset =
    MAPPER.readValue(suoritukset, classOf[VirtaSuoritukset])
}

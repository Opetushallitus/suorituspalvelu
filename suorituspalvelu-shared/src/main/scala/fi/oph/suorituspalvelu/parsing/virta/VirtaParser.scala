package fi.oph.suorituspalvelu.parsing.virta

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.{DeserializationContext, DeserializationFeature, JsonDeserializer, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

import java.io.{ByteArrayInputStream, InputStream}
import java.time.LocalDate

case class Header()

case class Tila(AlkuPvm: LocalDate, LoppuPvm: LocalDate, Koodi: String)

@JsonDeserialize(classOf[KoulutusalaDeserializer])
case class OpiskeluoikeusKoulutusala(versio: String, koodi: Int)

case class Laajuus(Opintopiste: BigDecimal)

case class Jakso(
  @JacksonXmlElementWrapper(useWrapping = false) Nimi: Seq[Nimi],
  Rahoituslahde: Option[String],
  Koulutuskieli: Option[String],
  LoppuPvm: LocalDate,
  AlkuPvm: LocalDate,
  Koulutuskoodi: Option[String],
  koulutusmoduulitunniste: String,
  Koulutuskunta: Option[String]
)

case class Opiskeluoikeus(
  Laajuus: Laajuus,
  LoppuPvm: LocalDate,
  koulutusmoduulitunniste: String = "",
  @JacksonXmlElementWrapper(useWrapping = false) Tila: Seq[Tila],
  @JacksonXmlElementWrapper(useWrapping = false) Jakso: Seq[Jakso],
  Koulutusala: OpiskeluoikeusKoulutusala,
  Tyyppi: String,
  AlkuPvm: LocalDate,
  Myontaja: String,
  opiskelijaAvain: String,
  avain: String
)

case class LukukausiIlmoittautuminen(IlmoittautumisPvm: LocalDate, opiskelijaAvain: String, LoppuPvm: LocalDate, Tila: String, opiskeluoikeusAvain: String, AlkuPvm: LocalDate, Myontaja: String)

case class Organisaatio(Rooli: String, Koodi: String, Osuus: Option[BigDecimal])

case class MuuAsteikkoArvosana(avain: String, Koodi: String, Nimi: String)

@JsonDeserialize(classOf[ArvosanaDeserializer])
case class Arvosana(arvosana: String, asteikko: String)

@JsonDeserialize(classOf[KoodiDeserializer])
case class Koodi(versio: String, koodi: Int)

case class SuoritusKoulutusala(Koodi: Koodi)

@JsonDeserialize(classOf[NimiDeserializer])
case class Nimi(kieli: Option[String], nimi: String)

@JsonDeserialize(classOf[SisaltyvyysDeserializer])
case class Suoritusviite(avain: String)

case class Opintosuoritus(
                           Kieli: String,
                           Organisaatio: Option[Organisaatio],
                           Tyyppi: String,
                           SuoritusPvm: LocalDate,
                           Arvosana: Option[Arvosana],
                           opiskeluoikeusAvain: Option[String], // puuttuu osasuorituksilta
                           Koulutusala: Option[SuoritusKoulutusala],
                           Laji: Int,
                           koulutusmoduulitunniste: String,
                           Opinnaytetyo: Option[String],
                           Laajuus: Laajuus,
                           HyvaksilukuPvm: Option[LocalDate],
                           opiskelijaAvain: String,
                           avain: String,
                           Myontaja: String,
                           Koulutuskoodi: Option[String],
                           @JacksonXmlElementWrapper(useWrapping = false) Nimi: Seq[Nimi],
                           TKILaajuusHarjoittelu: Option[Laajuus],
                           @JacksonXmlElementWrapper(useWrapping = false) Sisaltyvyys: Seq[Suoritusviite]
                         )

case class Opiskelija(Opiskeluoikeudet: Seq[Opiskeluoikeus], LukukausiIlmoittautumiset: Seq[LukukausiIlmoittautuminen], Opintosuoritukset: Option[Seq[Opintosuoritus]], Henkilotunnus: String, avain: String)

case class OpiskelijanKaikkiTiedotResponse(Virta: Seq[Opiskelija])

case class Body(OpiskelijanKaikkiTiedotResponse: OpiskelijanKaikkiTiedotResponse)

case class VirtaSuoritukset(Header: Header, Body: Body)

class KoulutusalaDeserializer extends JsonDeserializer[OpiskeluoikeusKoulutusala] {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): OpiskeluoikeusKoulutusala =
    val mixin = p.readValueAs(classOf[KoulutusAlaTaiKoodiMixIn])
    OpiskeluoikeusKoulutusala(mixin.versio, mixin.value)
}

class KoodiDeserializer extends JsonDeserializer[Koodi] {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): Koodi =
    val mixin = p.readValueAs(classOf[KoulutusAlaTaiKoodiMixIn])
    Koodi(mixin.versio, mixin.value)
}

class SisaltyvyysDeserializer extends JsonDeserializer[Suoritusviite] {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): Suoritusviite =
    val value = p.readValueAs(classOf[Any])
    value match
      case sisaltyvyys: Map[_, _] =>
        val sisaltyvyysMap = sisaltyvyys.asInstanceOf[Map[String, String]]
        Suoritusviite(sisaltyvyysMap("sisaltyvaOpintosuoritusAvain"))
}

class NimiDeserializer extends JsonDeserializer[Nimi] {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): Nimi =
    val value = p.readValueAs(classOf[Any])
    value match
      // jos tullaan tähän haaraan, nimiä on vain yksi ja sillä ei ole kieliattribuuttia
      case nimi: String => Nimi(None, nimi)
      // jos tullaan tähän haaraan kieliä on useita ja niillä pitäisi olla kieliattribuutit, jos ei ole niin räjähdetään
      case nimi: Map[_, _] =>
        val nimiMap = nimi.asInstanceOf[Map[String, String]]
        Nimi(Some(nimiMap("kieli")), nimiMap(""))
}

class ArvosanaDeserializer extends JsonDeserializer[Arvosana] {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): Arvosana =
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
          case list: List[_] =>
            list.map(item => mapper.convertValue(item, classOf[MuuAsteikkoArvosana]))
          case single: Map[_, _] =>
            Seq(mapper.convertValue(single, classOf[MuuAsteikkoArvosana]))
        }

        val matchingArvosana = asteikkoArvosanat.find(_.avain == koodi).map(_.Nimi)
        matchingArvosana match {
          case Some(arvosanaNimi) => Arvosana(arvosana = arvosanaNimi, asteikko = asteikkoNimi)
          case None => null
        }
      case _ => Arvosana(arvosana = arvosanaContent.asInstanceOf[String], asteikko = arvosanaTagName)
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

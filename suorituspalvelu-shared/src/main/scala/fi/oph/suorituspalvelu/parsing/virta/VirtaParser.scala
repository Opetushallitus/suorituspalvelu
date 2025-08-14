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

case class Tila(AlkuPvm: LocalDate, Koodi: String)

@JsonDeserialize(classOf[KoulutusalaDeserializer])
case class OpiskeluoikeusKoulutusala(versio: String, koodi: Int)

case class Laajuus(Opintopiste: BigDecimal)

case class Jakso(Rahoituslahde: String, Koulutuskieli: String, LoppuPvm: LocalDate, AlkuPvm: LocalDate, Koulutuskoodi: String, koulutusmoduulitunniste: String, Koulutuskunta: String)

case class OpiskeluOikeus(Laajuus: Laajuus, LoppuPvm: LocalDate, Tila: Tila, Jakso: Jakso, Koulutusala: OpiskeluoikeusKoulutusala, Tyyppi: String, AlkuPvm: LocalDate, Myontaja: String, opiskelijaAvain: String, avain: String)

case class LukukausiIlmoittautuminen(IlmoittautumisPvm: LocalDate, opiskelijaAvain: String, LoppuPvm: LocalDate, Tila: String, opiskeluoikeusAvain: String, AlkuPvm: LocalDate, Myontaja: String)

case class Organisaatio(Rooli: String, Koodi: String, Osuus: Option[BigDecimal])

@JsonDeserialize(classOf[ArvosanaDeserializer])
case class Arvosana(arvosana: String, asteikko: String)

@JsonDeserialize(classOf[KoodiDeserializer])
case class Koodi(versio: String, koodi: Int)

case class SuoritusKoulutusala(Koodi: Koodi)

@JsonDeserialize(classOf[NimiDeserializer])
case class Nimi(kieli: Option[String], nimi: String)

case class Opintosuoritus(
                           Kieli: String,
                           Organisaatio: Option[Organisaatio],
                           SuoritusPvm: LocalDate,
                           Arvosana: Option[Arvosana],
                           opiskeluoikeusAvain: String,
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
                           TKILaajuusHarjoittelu: Option[Laajuus]
                         )

case class Opiskelija(Opiskeluoikeudet: Seq[OpiskeluOikeus], LukukausiIlmoittautumiset: Seq[LukukausiIlmoittautuminen], Opintosuoritukset: Option[Seq[Opintosuoritus]], Henkilotunnus: String, avain: String)

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
    val tuple = p.readValueAs(classOf[Map[String, String]]).head
    if("EiKaytossa".equals(tuple._1))
      null
    else
      Arvosana(tuple._2, tuple._1)
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

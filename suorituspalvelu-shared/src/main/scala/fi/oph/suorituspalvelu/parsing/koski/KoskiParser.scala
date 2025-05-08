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
  def koodistoVersio: Int
}

case class Nimi(fi: String)

case class Arvosana(koodiarvo: String, koodistoUri: String, nimi: Nimi, koodistoVersio: Int) extends VersioituTunniste

case class Arviointi(arvosana: Arvosana, hyväksytty: Boolean)

case class Laajuus(arvo: Int, yksikkö: Option[Yksikko])

case class OsaSuoritus(tyyppi: SuoritusTyyppi, koulutusmoduuli: Option[KoulutusModuuli], arviointi: Option[Set[Arviointi]], osasuoritukset: Option[Set[OsaSuoritus]])

case class SuoritusKieli(koodiarvo: String, koodistoUri: String)

case class SuoritusTapa(koodiarvo: String, koodistoUri: String, koodistoVersio: Int, nimi: Nimi) extends VersioituTunniste

case class Yksikko(koodiarvo: String, koodistoUri: String, koodistoVersio: Int, nimi: Nimi) extends VersioituTunniste

case class KoulutusModuuliTunniste(koodiarvo: String, koodistoUri: String, koodistoVersio: Int, nimi: Nimi) extends VersioituTunniste

case class KoulutusModuuli(tunniste: KoulutusModuuliTunniste, laajuus: Option[Laajuus])

case class SuoritusTyyppi(koodiarvo: String, koodistoUri: String, nimi: Nimi)

case class Suoritus(tyyppi: SuoritusTyyppi, koulutusmoduuli: Option[KoulutusModuuli], suorituskieli: SuoritusKieli, vahvistuspäivä: Option[String], osasuoritukset: Option[Set[OsaSuoritus]], arviointi: Option[Set[Arviointi]], keskiarvo: Option[BigDecimal], suoritustapa: Option[SuoritusTapa])

case class OpiskeluoikeusJaksoTila(koodiarvo: String, koodistoUri: String, koodistoVersio: Int) extends VersioituTunniste

case class OpiskeluoikeusJakso(alku: LocalDate, tila: OpiskeluoikeusJaksoTila)

case class OpiskeluoikeusTila(opiskeluoikeusjaksot: List[OpiskeluoikeusJakso])

case class OpiskeluoikeusTyyppi(koodiarvo: String, koodistoUri: String)

case class Opiskeluoikeus(tyyppi: OpiskeluoikeusTyyppi, tila: Option[OpiskeluoikeusTila], suoritukset: Set[Suoritus])

case class SplitattavaKoskiData(oppijaOid: String, opiskeluoikeudet: Seq[Map[String, Any]])

/**
 * Parseroi Kosken JSON-muotoisen opiskeluoikeus-suoritusdatan KOSKI-spesifiksi objektipuuksi
 */
object KoskiParser {
  val MAPPER: ObjectMapper = {
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper.registerModule(new JavaTimeModule())
    mapper.registerModule(new Jdk8Module()) // tämä on java.util.Optional -kenttiä varten
    mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    mapper.configure(SerializationFeature.INDENT_OUTPUT, true)
    mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
    mapper
  }

  def parseKoskiData(suoritukset: String): Seq[Opiskeluoikeus] =
    MAPPER.readValue(suoritukset, classOf[Array[Opiskeluoikeus]]).toSeq

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

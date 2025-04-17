package fi.oph.suorituspalvelu.business.parsing

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fi.oph.suorituspalvelu.business.parsing.KoskiParsing.MAPPER
import org.junit.jupiter.api.{Assertions, Test}

object KoskiParsing {
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
}

case class Nimi(fi: String)

case class Arvosana(koodiarvo: String, koodistoUri: String, nimi: Nimi, koodistoVersio: Int)

case class Arviointi(arvosana: Arvosana, hyväksytty: Boolean)

case class OsaSuoritus(tyyppi: SuoritusTyyppi, koulutusmoduuli: KoulutusModuuli, arviointi: Set[Arviointi])

case class SuoritusKieli(koodiarvo: String, koodistoUri: String)

case class Yksikko(koodiarvo: String, koodistoUri: String, koodistoVersio: Int, nimi: Nimi)

case class Laajuus(arvo: Int, yksikkö: Yksikko)

case class KoulutusModuuliTunniste(koodiarvo: String, koodistoUri: String, koodistoVersio: Int, nimi: Nimi)

case class KoulutusModuuli(tunniste: KoulutusModuuliTunniste, laajuus: Option[Laajuus])

case class SuoritusTyyppi(koodiarvo: String, koodistoUri: String, nimi: Nimi)

case class Suoritus(tyyppi: SuoritusTyyppi, koulutusmoduuli: KoulutusModuuli, suorituskieli: SuoritusKieli, osasuoritukset: Set[OsaSuoritus])

case class OpiskeluoikeusJaksoTila(koodiarvo: String, koodistoUri: String)

case class OpiskeluoikeusJakso(tila: OpiskeluoikeusJaksoTila)

case class OpiskeluoikeusTila(opiskeluoikeusjaksot: Set[OpiskeluoikeusJakso])

case class OpiskeluoikeusTyyppi(koodiarvo: String, koodistoUri: String)

case class Opiskeluoikeus(tyyppi: OpiskeluoikeusTyyppi, tila: OpiskeluoikeusTila, suoritukset: Set[Suoritus])

case class KoskiSuoritukset(oppijaOid: String, opiskeluoikeudet: Seq[Opiskeluoikeus])

case class NormalisoitavaKoskiData(oppijaOid: String, opiskeluoikeudet: Seq[Map[String, Any]])

@Test
class KoskiParsingTest {

  @Test def testKoskiNormalization(): Unit =
    val suoritukset = MAPPER. readValue(this.getClass.getResourceAsStream("/koski0.json"), classOf[Array[NormalisoitavaKoskiData]])

    val s = ""

  @Test def testKoskiParsing(): Unit =
    val suoritukset = MAPPER.readValue(this.getClass.getResourceAsStream("/koski0.json"), classOf[Array[KoskiSuoritukset]])
    val s = ""

  @Test def testKoskiParsing2(): Unit =
    val suoritukset = MAPPER.readValue(this.getClass.getResourceAsStream("/1_2_246_562_24_40483869857.json"), classOf[Array[KoskiSuoritukset]])
    val s = ""


}

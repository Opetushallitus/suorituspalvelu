package fi.oph.suorituspalvelu.parsing.koski

import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import java.io.InputStream
import java.time.LocalDate

trait KoskiVersioituTunniste {
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
                      koodistoVersio: Option[Int],
                      nimi: Kielistetty,
                      lyhytNimi: Option[Kielistetty]) extends KoskiVersioituTunniste

case class KoskiArviointi(arvosana: KoskiKoodi,
                          `päivä`: Option[String],
                          hyväksytty: Boolean)

case class KoskiLaajuus(arvo: BigDecimal,
                        yksikkö: Option[KoskiKoodi])

case class KoskiOsaSuoritus(tyyppi: KoskiSuoritusTyyppi,
                            koulutusmoduuli: Option[KoskiKoulutusModuuli],
                            arviointi: Option[Set[KoskiArviointi]],
                            `yksilöllistettyOppimäärä`: Option[Boolean],
                            `rajattuOppimäärä`: Option[Boolean],
                            suorituskieli: Option[KoskiSuoritusKieli],
                            osasuoritukset: Option[Set[KoskiOsaSuoritus]])

case class KoskiSuoritusKieli(koodiarvo: String,
                              koodistoUri: String,
                              koodistoVersio: Option[Int]) extends KoskiVersioituTunniste

case class KoskiSuoritusTapa(koodiarvo: String,
                             koodistoUri: String,
                             koodistoVersio: Option[Int],
                             nimi: Kielistetty) extends KoskiVersioituTunniste

case class KoskiKoulutusModuuli(tunniste: Option[KoskiKoodi],
                                koulutustyyppi: Option[KoskiKoodi],
                                laajuus: Option[KoskiLaajuus],
                                kieli: Option[KoskiKoodi],
                                pakollinen: Option[Boolean])

case class KoskiSuoritusTyyppi(koodiarvo: String,
                               koodistoUri: String,
                               nimi: Kielistetty)

case class KoskiVahvistus(`päivä`: String)

case class KoskiSuoritus(tyyppi: KoskiSuoritusTyyppi,
                         koulutusmoduuli: Option[KoskiKoulutusModuuli],
                         suorituskieli: Option[KoskiSuoritusKieli],
                         koulusivistyskieli: Option[Set[KoskiSuoritusKieli]],
                         alkamispäivä: Option[String],
                         vahvistus: Option[KoskiVahvistus],
                         osasuoritukset: Option[Set[KoskiOsaSuoritus]],
                         arviointi: Option[Set[KoskiArviointi]],
                         keskiarvo: Option[BigDecimal],
                         suoritustapa: Option[KoskiSuoritusTapa],
                         luokka: Option[String],
                         `jääLuokalle`: Option[Boolean])

case class KoskiOpiskeluoikeusJakso(alku: LocalDate,
                                    tila: KoskiKoodi)

case class KoskiOpiskeluoikeusTila(opiskeluoikeusjaksot: List[KoskiOpiskeluoikeusJakso])

case class KoskiOpiskeluoikeusTyyppi(koodiarvo: String,
                                     koodistoUri: String,
                                     koodistoVersio: Option[Int]) extends KoskiVersioituTunniste

case class KoskiOppilaitos(nimi: Kielistetty,
                           oid: String)

case class KoskiErityisenTuenPaatos(opiskeleeToimintaAlueittain: Option[Boolean])

case class KoskiKotiopetusjakso(alku: String,
                                loppu: Option[String])

case class KoskiLisatiedot(erityisenTuenPäätökset: Option[List[KoskiErityisenTuenPaatos]],
                           vuosiluokkiinSitoutumatonOpetus: Option[Boolean],
                           kotiopetusjaksot: Option[List[KoskiKotiopetusjakso]])

case class KoskiOpiskeluoikeus(oid: String,
                               oppilaitos: Option[KoskiOppilaitos],
                               tyyppi: Option[KoskiOpiskeluoikeusTyyppi],
                               tila: Option[KoskiOpiskeluoikeusTila],
                               suoritukset: Option[Set[KoskiSuoritus]],
                               lisätiedot: Option[KoskiLisatiedot],
                               poistettu: Option[Boolean]) {
  def isPerusopetus: Boolean = tyyppi.exists(t => t.koodiarvo == "perusopetus" || t.koodiarvo == "aikuistenperusopetus")
  def isAmmatillinen: Boolean = tyyppi.exists(_.koodiarvo == "ammatillinenkoulutus")
}

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

  def parseKoskiData(data: String): KoskiOpiskeluoikeus =
    MAPPER.readValue(data, classOf[KoskiOpiskeluoikeus])

}

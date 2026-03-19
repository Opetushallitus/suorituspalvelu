package fi.oph.suorituspalvelu.parsing

import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.suorituspalvelu.business.*
import fi.oph.suorituspalvelu.parsing.koski.{KoskiParser, KoskiToSuoritusConverter, KoskiUtil}
import fi.oph.suorituspalvelu.parsing.virkailija.VirkailijaToSuoritusConverter
import fi.oph.suorituspalvelu.parsing.virta.{VirtaParser, VirtaToSuoritusConverter}
import fi.oph.suorituspalvelu.parsing.ytr.{YtrParser, YtrToSuoritusConverter}
import fi.oph.suorituspalvelu.resource.ui.{
  SyotettyPerusopetuksenOppiaineenOppimaarienSuoritusContainer, SyotettyPerusopetuksenOppimaaranSuoritus
}
import fi.oph.suorituspalvelu.util.{KoodistoProvider, OrganisaatioProvider}
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import java.time.Instant

/**
 * Palvelu opiskeluoikeuksien on-demand-parserointiin.
 *
 * Käytetään tilanteissa joissa versio on tallennettu mutta opiskeluoikeuksia ei ole vielä parsittu
 * (eli tallennus ja parserointi tapahtuivat eri transaktioissa ja lukija osuu tähän väliin).
 */
@Service
class OpiskeluoikeusParsingService(
  kantaOperaatiot: KantaOperaatiot,
  koodistoProvider: KoodistoProvider,
  organisaatioProvider: OrganisaatioProvider,
  objectMapper: ObjectMapper
) {

  private val LOG = LoggerFactory.getLogger(classOf[OpiskeluoikeusParsingService])

  private val SUORITUS_MAPPER: ObjectMapper = {
    val mapper = KantaOperaatiot.MAPPER.copy()
    mapper.registerSubtypes(
      classOf[PerusopetuksenOpiskeluoikeus],
      classOf[PerusopetuksenOppimaara],
      classOf[PerusopetukseenValmistavaOpetus],
      classOf[AmmatillinenOpiskeluoikeus],
      classOf[PoistettuOpiskeluoikeus],
      classOf[KKOpiskeluoikeus],
      classOf[KKSynteettinenOpiskeluoikeus],
      classOf[AmmatillinenPerustutkinto],
      classOf[AmmatillinenTutkintoOsittainen],
      classOf[AmmattiTutkinto],
      classOf[GeneerinenOpiskeluoikeus],
      classOf[YOOpiskeluoikeus],
      classOf[Telma],
      classOf[PerusopetuksenOppimaaranOppiaineidenSuoritus],
      classOf[Tuva],
      classOf[KKTutkinto],
      classOf[KKSynteettinenSuoritus],
      classOf[KKOpintosuoritus],
      classOf[VapaaSivistystyo],
      classOf[EBTutkinto],
      classOf[IBTutkinto],
      classOf[ErikoisAmmattiTutkinto],
      classOf[LukionOppimaara],
      classOf[DIATutkinto])
    mapper
  }

  /**
   * Hakee version raakadatan kannasta lähdejärjestelmän perusteella.
   * @return Tuple, jossa ensimmäisenä JSON-data, toisena XML-data. Toinen näistä on aina tyhjä, riippuen lähdejärjestelmästä.
   */
  private def haeData(versio: VersioEntiteetti): (Seq[String], Seq[String]) = {
    if (versio.lahdeJarjestelma.hasXmlData) {
      (Seq.empty[String], kantaOperaatiot.haeXmlData(versio))
    } else {
      (kantaOperaatiot.haeJsonData(versio), Seq.empty[String])
    }
  }

  /**
   * Parseroi version raakadatan opiskeluoikeuksiksi ja tallentaa tuloksen kantaan.
   *
   * @param versio versio jonka raakadata parseroidaan
   * @return parseroidut opiskeluoikeudet
   */
  def parseAndStore(versio: VersioEntiteetti): Set[Opiskeluoikeus] = {
    LOG.info(s"On-demand-parserointi versiolle ${versio.tunniste} (${versio.lahdeJarjestelma})")

    val (jsonData, xmlData) = haeData(versio)
    val (opiskeluoikeudet, parserVersio) = parse(versio, jsonData, xmlData)
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, opiskeluoikeudet, KoskiUtil.getLahtokouluMetadata(opiskeluoikeudet), parserVersio)
    opiskeluoikeudet
  }

  /**
   * Parseroi version raakadatan opiskeluoikeuksiksi ilman tallennusta.
   *
   * @param versio versio jonka raakadata parseroidaan
   * @return parseroidut opiskeluoikeudet
   */
  def parseOnly(versio: VersioEntiteetti): Set[Opiskeluoikeus] = {
    val (jsonData, xmlData) = haeData(versio)
    parse(versio, jsonData, xmlData)._1
  }

  /**
   * Hakee henkilön suoritukset ja parseroi on-demand jos tarvitaan.
   *
   * @param henkiloOid henkilön oid
   * @return suoritukset versioittain
   */
  def haeSuoritukset(henkiloOid: String): Map[VersioEntiteetti, Set[Opiskeluoikeus]] = {
    haeSuorituksetAjanhetkella(henkiloOid, Instant.now())
  }

  /**
   * Hakee henkilön suoritukset tietyllä ajanhetkellä ja parseroi on-demand jos tarvitaan.
   *
   * Parserointi tapahtuu seuraavissa tilanteissa:
   * - Versiota ei ole vielä parseroitu (parserVersio puuttuu)
   * - Tallennettu parserVersio on vanhempi kuin nykyinen -> parsitaan ja tallennetaan
   * - Tallennettu parserVersio on uudempi kuin nykyinen (deployment-tilanne) -> parsitaan mutta ei tallenneta
   *
   * @param henkiloOid henkilön oid
   * @param timestamp  ajanhetki
   * @return suoritukset versioittain
   */
  def haeSuorituksetAjanhetkella(henkiloOid: String, timestamp: Instant): Map[VersioEntiteetti, Set[Opiskeluoikeus]] = {
    kantaOperaatiot.haeSuorituksetAjanhetkellaUnparsed(henkiloOid, timestamp).map { case (versio, opiskeluoikeusContainerRaw) =>
      val currentParserVersion = ParserVersions.forLahdejarjestelma(versio.lahdeJarjestelma)
      versio.parserVersio match {
        case None =>
          // Versio ei ole vielä parsittu, parsitaan on-demand ja tallennetaan
          LOG.info(s"Versio ${versio.tunniste} ei ole vielä parseroitu, parseroidaan on-demand")
          val parsed = parseAndStore(versio)
          (versio.copy(parserVersio = Some(currentParserVersion)), parsed)

        case Some(storedVersion) if storedVersion < currentParserVersion =>
          // Tallennettu versio on vanhempi, parsitaan uudelleen ja tallennetaan
          LOG.info(s"Versio ${versio.tunniste} on parseroitu vanhemmalla versiolla ($storedVersion < $currentParserVersion), parseroidaan uudelleen")
          val parsed = parseAndStore(versio)
          (versio.copy(parserVersio = Some(currentParserVersion)), parsed)

        case Some(storedVersion) if storedVersion > currentParserVersion =>
          // Tallennettu versio on uudempi (deployment-tilanne), parsitaan mutta ei tallenneta
          LOG.info(s"Versio ${versio.tunniste} on parseroitu uudemmalla versiolla ($storedVersion > $currentParserVersion), parseroidaan ilman tallennusta")
          val parsed = parseOnly(versio)
          (versio, parsed)

        case _ =>
          // Versiot täsmäävät, käytetään tallennettuja opiskeluoikeuksia
          // Parseroidaan aiemmin tallennetut opiskeluoikeudet vasta tässä, jotta ei kaaduta vanhaan epäyhteensopivaan dataan
          val opiskeluoikeudet = SUORITUS_MAPPER.readValue(opiskeluoikeusContainerRaw, classOf[Container]).opiskeluoikeudet
          (versio, opiskeluoikeudet)
      }
    }
  }

  private def parse(versio: VersioEntiteetti, jsonData: Seq[String], xmlData: Seq[String]): (Set[Opiskeluoikeus], Int) = {
    versio.lahdeJarjestelma match {
      case Lahdejarjestelma.KOSKI =>
        val parsed = jsonData.map(d => KoskiParser.parseKoskiData(d))
        val converted = KoskiToSuoritusConverter.parseOpiskeluoikeudet(parsed, koodistoProvider).toSet
        (converted, ParserVersions.KOSKI)

      case Lahdejarjestelma.VIRTA =>
        val parsed = xmlData.flatMap(VirtaParser.parseVirtaOpiskelijat)
        val converted: Set[Opiskeluoikeus] = VirtaToSuoritusConverter.toOpiskeluoikeudet(parsed).toSet
        (converted, ParserVersions.VIRTA)

      case Lahdejarjestelma.YTR =>
        val parsed = jsonData.map(d => YtrParser.parseYtrData(d))
        val converted: Set[Opiskeluoikeus] = parsed.map(s => YtrToSuoritusConverter.toSuoritus(s)).toSet
        (converted, ParserVersions.YTR)

      case Lahdejarjestelma.SYOTETTY_PERUSOPETUS =>
        val parsed = jsonData.map(d => objectMapper.readValue(d, classOf[SyotettyPerusopetuksenOppimaaranSuoritus]))
        val converted: Set[Opiskeluoikeus] = parsed.map(p => VirkailijaToSuoritusConverter.toPerusopetuksenOppimaara(versio.tunniste, p, koodistoProvider, organisaatioProvider)).toSet
        (converted, ParserVersions.SYOTETTY_PERUSOPETUS)

      case Lahdejarjestelma.SYOTETYT_OPPIAINEET =>
        val parsed = jsonData.map(d => objectMapper.readValue(d, classOf[SyotettyPerusopetuksenOppiaineenOppimaarienSuoritusContainer]))
        val converted: Set[Opiskeluoikeus] = parsed.map(p => VirkailijaToSuoritusConverter.toPerusopetuksenOppiaineenOppimaara(versio.tunniste, p, koodistoProvider, organisaatioProvider)).toSet
        (converted, ParserVersions.SYOTETYT_OPPIAINEET)

      case _ =>
        LOG.error(s"Tuntematon lähdejärjestelmä: ${versio.lahdeJarjestelma.nimi}")
        (Set.empty, 0)
    }
  }
}

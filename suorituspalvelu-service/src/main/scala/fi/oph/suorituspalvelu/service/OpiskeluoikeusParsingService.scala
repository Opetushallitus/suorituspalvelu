package fi.oph.suorituspalvelu.service

import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.suorituspalvelu.business.{KantaOperaatiot, Opiskeluoikeus, ParserVersions, SuoritusJoukko, VersioEntiteetti}
import fi.oph.suorituspalvelu.parsing.koski.{KoskiParser, KoskiToSuoritusConverter, KoskiUtil}
import fi.oph.suorituspalvelu.parsing.virkailija.VirkailijaToSuoritusConverter
import fi.oph.suorituspalvelu.parsing.virta.{VirtaParser, VirtaToSuoritusConverter}
import fi.oph.suorituspalvelu.parsing.ytr.{YtrParser, YtrToSuoritusConverter}
import fi.oph.suorituspalvelu.resource.ui.{SyotettyPerusopetuksenOppiaineenOppimaarienSuoritusContainer, SyotettyPerusopetuksenOppimaaranSuoritus}
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

  /**
   * Parseroi version raakadatan opiskeluoikeuksiksi ja tallentaa tuloksen kantaan.
   *
   * @param versio versio jonka raakadata parseroidaan
   * @return parseroidut opiskeluoikeudet
   */
  def parseAndStore(versio: VersioEntiteetti): Set[Opiskeluoikeus] = {
    LOG.info(s"On-demand-parserointi versiolle ${versio.tunniste} (${versio.suoritusJoukko})")
    val (_, jsonData, xmlData) = kantaOperaatiot.haeData(versio)
    val (opiskeluoikeudet, parserVersio) = parse(versio, jsonData, xmlData)
    kantaOperaatiot.tallennaVersioonLiittyvatEntiteetit(versio, opiskeluoikeudet, KoskiUtil.getLahtokouluMetadata(opiskeluoikeudet), parserVersio)
    opiskeluoikeudet
  }

  /**
   * Parseroi version raakadatan opiskeluoikeuksiksi ilman tallennusta.
   *
   * @param versio versio jonka raakadata parsesoidaan
   * @return parseroidut opiskeluoikeudet
   */
  def parseOnly(versio: VersioEntiteetti): Set[Opiskeluoikeus] = {
    val (_, jsonData, xmlData) = kantaOperaatiot.haeData(versio)
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
    val result = kantaOperaatiot.haeSuorituksetAjanhetkella(henkiloOid, timestamp)
    result.map { case (versio, opiskeluoikeudet) =>
      val currentParserVersion = ParserVersions.forSuoritusJoukko(versio.suoritusJoukko)
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
          (versio, opiskeluoikeudet)
      }
    }
  }

  private def parse(versio: VersioEntiteetti, jsonData: Seq[String], xmlData: Seq[String]): (Set[Opiskeluoikeus], Int) = {
    versio.suoritusJoukko match {
      case SuoritusJoukko.KOSKI =>
        val parsed = jsonData.flatMap(d => KoskiParser.parseKoskiData(d))
        val converted = KoskiToSuoritusConverter.parseOpiskeluoikeudet(parsed, koodistoProvider).toSet
        (converted, ParserVersions.KOSKI)

      case SuoritusJoukko.VIRTA =>
        val parsed = xmlData.map(d => VirtaParser.parseVirtaData(d))
        val converted: Set[Opiskeluoikeus] = parsed.flatMap(p => VirtaToSuoritusConverter.toOpiskeluoikeudet(p)).toSet
        (converted, ParserVersions.VIRTA)

      case SuoritusJoukko.YTR =>
        val parsed = jsonData.map(d => YtrParser.parseYtrData(d))
        val converted: Set[Opiskeluoikeus] = parsed.map(s => YtrToSuoritusConverter.toSuoritus(s)).toSet
        (converted, ParserVersions.YTR)

      case SuoritusJoukko.SYOTETTY_PERUSOPETUS =>
        val parsed = jsonData.map(d => objectMapper.readValue(d, classOf[SyotettyPerusopetuksenOppimaaranSuoritus]))
        val converted: Set[Opiskeluoikeus] = parsed.map(p => VirkailijaToSuoritusConverter.toPerusopetuksenOppimaara(versio.tunniste, p, koodistoProvider, organisaatioProvider)).toSet
        (converted, ParserVersions.SYOTETTY_PERUSOPETUS)

      case SuoritusJoukko.SYOTETYT_OPPIAINEET =>
        val parsed = jsonData.map(d => objectMapper.readValue(d, classOf[SyotettyPerusopetuksenOppiaineenOppimaarienSuoritusContainer]))
        val converted: Set[Opiskeluoikeus] = parsed.map(p => VirkailijaToSuoritusConverter.toPerusopetuksenOppiaineenOppimaara(versio.tunniste, p, koodistoProvider, organisaatioProvider)).toSet
        (converted, ParserVersions.SYOTETYT_OPPIAINEET)

      case _ =>
        LOG.warn(s"Tuntematon suoritusjoukko: ${versio.suoritusJoukko}")
        (Set.empty, 0)
    }
  }
}

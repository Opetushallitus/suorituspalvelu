package fi.oph.suorituspalvelu.yos

import fi.oph.suorituspalvelu.business.{KKOpiskeluoikeus, Lahdejarjestelma, Opiskeluoikeus}
import fi.oph.suorituspalvelu.integration.TarjontaIntegration
import fi.oph.suorituspalvelu.integration.client.{KoutaHaku, KoutaHakukohde}
import fi.oph.suorituspalvelu.parsing.OpiskeluoikeusParsingService
import fi.oph.suorituspalvelu.parsing.koski.Kielistetty
import fi.oph.suorituspalvelu.parsing.virta.VirtaToSuoritusConverter
import fi.oph.suorituspalvelu.resource.api.YosVirhe.{VIRHE_HAKUTOIVEEN_PAATTELYSSA, VIRHE_PAATETTAVIEN_OPISKELUOIKEUKSIEN_HAUSSA}
import fi.oph.suorituspalvelu.resource.api.{YosErrorResponse, YosSuccessResponse}
import fi.oph.suorituspalvelu.util.OrganisaatioProvider
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import java.lang

@Service
class YosService @Autowired (tarjontaIntegration: TarjontaIntegration,
                             opiskeluOikeusService: OpiskeluoikeusParsingService,
                             organisaatioProvider: OrganisaatioProvider) {

  private val LOGGER = LoggerFactory.getLogger(classOf[YosService])

  def haeHakijanPaatettavatOpiskeluOikeudet(hakijaOid: String, hakuOid: String, hakukohdeOid: String): Either[YosErrorResponse, Set[YosPaatettavaOpiskeluOikeus]] = {
    LOGGER.info(s"Tarkistetaan kuuluuko vastaanotettava opiskelupaikka YOS piiriin. Parametrit = (hakija: $hakijaOid, haku: $hakuOid, hakukohde: $hakukohdeOid)")
    kuuluukoVastaanotettavaHakutoiveYossinpiiriin(hakuOid, hakukohdeOid).fold(
      e => Left(YosErrorResponse(VIRHE_HAKUTOIVEEN_PAATTELYSSA, e.getMessage)),
      r => Right(r)
    ).flatMap(kuuluuYosPiiriin => {
      if (kuuluuYosPiiriin) {
        LOGGER.info(s"Vastaanotettava opiskelupaikka kuului YOS piiriin. Haetaan päätettävät opiskeluoikeudet. Parametrit = (hakija: $hakijaOid, haku: $hakuOid, hakukohde: $hakukohdeOid)")
        hakijanPaatettavatOpiskeluOikeudet(hakijaOid).fold(
          e => Left(YosErrorResponse(VIRHE_PAATETTAVIEN_OPISKELUOIKEUKSIEN_HAUSSA, e.getMessage)),
          r => Right(r))
      } else {
        LOGGER.info(s"Vastaanotettava opiskelupaikka ei kuulunut YOS piiriin. Palautetaan tyhjä lista. Parametrit = (hakija: $hakijaOid, haku: $hakuOid, hakukohde: $hakukohdeOid)")
        Right(Set.empty)
      }
    })
  }

  def kuuluukoVastaanotettavaHakutoiveYossinpiiriin(hakuOid: String, hakukohdeOid: String): Either[Throwable, Boolean] = {
    LOGGER.info(s"Tehdään päättely kuuluuko hakutoive $hakukohdeOid haussa $hakuOid YOS piiriin")
    try {
      val haku: Option[KoutaHaku] = tarjontaIntegration.getHaku(hakuOid)
      val hakutoive: KoutaHakukohde = tarjontaIntegration.getHakukohde(hakukohdeOid)
      (haku, hakutoive) match {
        case (None, _) =>
          LOGGER.error(s"Hakua ei löydy oidilla: $hakuOid")
          Left(new RuntimeException(s"Hakua ei löydy oidilla: $hakuOid"))
        case (Some(_), null) =>
          LOGGER.error(s"Hakukohdetta ei löydy oidilla: $hakukohdeOid")
          Left(new RuntimeException(s"Hakukohdetta ei löydy oidilla: $hakukohdeOid"))
        case (Some(h), hk) =>
          val yosHakutoive = muodostaYosHakutoive(h, hakutoive)
          val kuuluukoYOSsinPiiriin = YosPredicate.kuuluukoHakutoiveYosinPiiriin(yosHakutoive)
          LOGGER.info(s"Hakutoive $hakukohdeOid haussa $hakuOid ${if (kuuluukoYOSsinPiiriin) "kuuluu" else "ei kuulu"} YOS piiriin")
          Right(kuuluukoYOSsinPiiriin)
      }
    } catch {
      case e: Exception =>
        LOGGER.error(s"Virhe vastaanotettavan hakutoiveen päättelyssä haulle $hakuOid ja hakukohteelle $hakukohdeOid", e)
        Left(RuntimeException(s"Virhe vastaanotettavan hakutoiveen päättelyssä haulle $hakuOid ja hakukohteelle $hakukohdeOid", e))
      }
  }

  def hakijanPaatettavatOpiskeluOikeudet(oppilasNro: String): Either[Throwable, Set[YosPaatettavaOpiskeluOikeus]] = {
    try {
      LOGGER.info(s"Haetaan hakijan $oppilasNro päätettävät opiskeluoikeudet")
      val oikeudet: Set[Opiskeluoikeus] = opiskeluOikeusService.haeSuoritukset(oppilasNro)
        .filter((versio, _) => versio.lahdeJarjestelma == Lahdejarjestelma.VIRTA)
        .values.flatten
        .toSet
      LOGGER.info(s"Löytyi ${oikeudet.size} käsiteltävää oikeutta hakijalle $oppilasNro, suodatetaan niistä YOS piiriin kuuluvat")
      val paatettavatOikeudet = oikeudet.filter(oikeus => YosPredicate.kuuluukoOpiskeluoikeusYosinPiiriin(oikeus))
        .map(oikeus => oikeus.asInstanceOf[KKOpiskeluoikeus])
        .map(muodostaYosPaatettavaOpiskeluOikeus)
      LOGGER.info(s"Oikeuksista löytyi ${paatettavatOikeudet.size} kappaletta päätettävää oikeutta hakijalle $oppilasNro")
      Right(paatettavatOikeudet)
    } catch {
      case e: Exception =>
        LOGGER.error(s"Virhe hakiessa päätettäviä opiskeluoikeuksia hakijalle $oppilasNro", e)
        Left(new RuntimeException(s"Virhe hakiessa päätettäviä opiskeluoikeuksia hakijalle $oppilasNro", e))
    }
  }

  private def muodostaYosHakutoive(haku: KoutaHaku, hakutoive: KoutaHakukohde): YosHakutoive = {
    YosHakutoive(haku.isKorkeakouluHaku, hakutoive.johtaaTutkintoon.getOrElse(false), haku.isJatkotutkinto, haku.isErasmusMundusTaiKaksoistutkinto, "", "")
  }

  private def muodostaYosPaatettavaOpiskeluOikeus(oikeus: KKOpiskeluoikeus): YosPaatettavaOpiskeluOikeus = {
    val oppilaitosTiedot = organisaatioProvider.haeOrganisaationTiedot(oikeus.myontaja)
    val organisaatio = YosOrganisaatio(
      oppilaitosTiedot.map(org => org.oid),
      oppilaitosTiedot.map(org =>
        Kielistetty(
          Some(org.nimi.fi),
          Some(org.nimi.sv),
          Some(org.nimi.en)))
        .getOrElse(
          Kielistetty(
            Some(oikeus.myontaja),
            Some(oikeus.myontaja),
            Some(oikeus.myontaja)))
    )
    val virtaOpiskeluOikeusId = VirtaToSuoritusConverter.getVirtaOpiskeluoikeusId(oikeus.myontaja, oikeus.virtaTunniste)
    YosPaatettavaOpiskeluOikeus(virtaOpiskeluOikeusId, organisaatio, oikeus.nimi, oikeus.koulutusKoodi)
  }
}

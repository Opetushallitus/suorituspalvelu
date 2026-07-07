package fi.oph.suorituspalvelu.yos

import fi.oph.suorituspalvelu.business.{KKOpiskeluoikeus, Lahdejarjestelma, Opiskeluoikeus}
import fi.oph.suorituspalvelu.integration.TarjontaIntegration
import fi.oph.suorituspalvelu.integration.client.{KoutaHaku, KoutaHakukohde}
import fi.oph.suorituspalvelu.parsing.OpiskeluoikeusParsingService
import fi.oph.suorituspalvelu.parsing.koski.Kielistetty
import fi.oph.suorituspalvelu.parsing.virta.VirtaOpiskeluoikeus
import fi.oph.suorituspalvelu.resource.api.YosVirhe.{VIRHE_HAKUTOIVEEN_PAATTELYSSA, VIRHE_PAATETTAVIEN_OPISKELUOIKEUKSIEN_HAUSSA}
import fi.oph.suorituspalvelu.resource.api.YosErrorResponse
import fi.oph.suorituspalvelu.util.KoodistoConstants.KOULUTUS_KOODISTO
import fi.oph.suorituspalvelu.util.{KoodistoProvider, OrganisaatioProvider}
import fi.oph.suorituspalvelu.yos.YosConstants.{KOULUTUSASTE_ALEMMAT, KOULUTUSASTE_YLEMMAT, LAAKETIETEEN_LISENSIAATIT_KOULUTUSKOODIT, YOS_KOULUTUSASTE_KOODISTO}
import fi.oph.suorituspalvelu.yos.YosKoulutusAsteLuokka.{ALEMMAT_ASTEET, EI_YOS_KOULUTUSASTETTA, YLEMMAT_ASTEET, YLEMMAT_JA_ALEMMAT_ASTEET}
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import java.lang

case class YosHakuToiveYossinPiirissa(hakutoive: YosHakutoive, kuuluukoYosPiiriin: Boolean)

@Service
class YosService @Autowired (tarjontaIntegration: TarjontaIntegration,
                             opiskeluOikeusService: OpiskeluoikeusParsingService,
                             organisaatioProvider: OrganisaatioProvider,
                             koodistoProvider: KoodistoProvider) {

  private val LOGGER = LoggerFactory.getLogger(classOf[YosService])

  def haeHakijanPaatettavatOpiskeluOikeudet(hakijaOid: String, hakuOid: String, hakukohdeOid: String): Either[YosErrorResponse, Set[YosPaatettavaOpiskeluOikeus]] = {
    LOGGER.info(s"Tarkistetaan kuuluuko vastaanotettava opiskelupaikka YOS piiriin. Parametrit = (hakija: $hakijaOid, haku: $hakuOid, hakukohde: $hakukohdeOid)")
    kuuluukoVastaanotettavaHakutoiveYossinpiiriin(hakuOid, hakukohdeOid).fold(
      e => Left(YosErrorResponse(VIRHE_HAKUTOIVEEN_PAATTELYSSA, e.getMessage)),
      r => Right(r)
    ).flatMap(toiveJaPiiri => {
      if (toiveJaPiiri.kuuluukoYosPiiriin) {
        LOGGER.info(s"Vastaanotettava opiskelupaikka kuului YOS piiriin. Haetaan päätettävät opiskeluoikeudet. Parametrit = (hakija: $hakijaOid, haku: $hakuOid, hakukohde: $hakukohdeOid)")
        hakijanPaatettavatOpiskeluOikeudet(hakijaOid, toiveJaPiiri.hakutoive).fold(
          e => Left(YosErrorResponse(VIRHE_PAATETTAVIEN_OPISKELUOIKEUKSIEN_HAUSSA, e.getMessage)),
          r => Right(r))
      } else {
        LOGGER.info(s"Vastaanotettava opiskelupaikka ei kuulunut YOS piiriin. Palautetaan tyhjä lista. Parametrit = (hakija: $hakijaOid, haku: $hakuOid, hakukohde: $hakukohdeOid)")
        Right(Set.empty)
      }
    })
  }

  def kuuluukoVastaanotettavaHakutoiveYossinpiiriin(hakuOid: String, hakukohdeOid: String): Either[Throwable, YosHakuToiveYossinPiirissa] = {
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
          if (!hakuOid.equals(hk.hakuOid)) {
            LOGGER.error(s"Hakukohde $hakukohdeOid ei kuulu annettuun hakuun $hakuOid")
            Left(new RuntimeException(s"Hakukohde $hakukohdeOid ei kuulu annettuun hakuun $hakuOid"))
          } else {
            val yosHakutoive = muodostaYosHakutoive(h, hakutoive)
            val kuuluukoYOSsinPiiriin = YosPredicate.kuuluukoHakutoiveYosinPiiriin(yosHakutoive)
            LOGGER.info(s"Hakutoive $hakukohdeOid haussa $hakuOid ${if (kuuluukoYOSsinPiiriin) "kuuluu" else "ei kuulu"} YOS piiriin")
            Right(YosHakuToiveYossinPiirissa(yosHakutoive, kuuluukoYOSsinPiiriin))
          }
      }
    } catch {
      case e: Exception =>
        LOGGER.error(s"Virhe vastaanotettavan hakutoiveen päättelyssä haulle $hakuOid ja hakukohteelle $hakukohdeOid", e)
        Left(RuntimeException(s"Virhe vastaanotettavan hakutoiveen päättelyssä haulle $hakuOid ja hakukohteelle $hakukohdeOid", e))
      }
  }

  def hakijanPaatettavatOpiskeluOikeudet(oppilasNro: String, hakutoive: YosHakutoive): Either[Throwable, Set[YosPaatettavaOpiskeluOikeus]] = {
    try {
      LOGGER.info(s"Haetaan hakijan $oppilasNro päätettävät opiskeluoikeudet")
      val oikeudet: Set[KKOpiskeluoikeus] = opiskeluOikeusService.haeSuoritukset(oppilasNro)
        .filter((versio, _) => versio.lahdeJarjestelma == Lahdejarjestelma.VIRTA)
        .values.flatten
        .filter(oikeus => oikeus.isInstanceOf[KKOpiskeluoikeus])
        .map(oikeus => oikeus.asInstanceOf[KKOpiskeluoikeus])
        .toSet
      LOGGER.info(s"Löytyi ${oikeudet.size} käsiteltävää korkeakouluoikeutta hakijalle $oppilasNro, suodatetaan niistä YOS piiriin kuuluvat")
      val paatettavatOikeudet = oikeudet.filter(oikeus => {
          LOGGER.info(s"""Tarkistetaan kuuluuko opiskeluoikeus ${oikeus.virtaTunniste} yosin piiriin.
                |Opiskeluoikeuden arvot ovat:
                | virtatila: ${oikeus.virtaTila.arvo}, tutkintoon johtava: ${oikeus.isTutkintoonJohtava},
                | rahoituslahde: ${oikeus.rahoitusLahde.orNull}, opiskeluoikeus tyyppi: ${oikeus.tyyppiKoodi},
                | virta luokittelu: ${oikeus.luokittelu}""".stripMargin)
          val kuuluu = YosPredicate.kuuluukoOpiskeluoikeusYosinPiiriin(oikeus)
          if (kuuluu) {
            LOGGER.info(s"Opiskeluoikeus ${oikeus.virtaTunniste} kuuluu yosin piiriin")
          } else {
            LOGGER.info(s"Opiskeluoikeus ${oikeus.virtaTunniste} ei kuulu yosin piiriin")
          }
          kuuluu
        })
        .filter(oikeus => {
          val parentOrganisaatiot = organisaatioProvider.haeKaikkiOrganisaationParenttienOidit(oikeus.myontaja)
          YosPredicate.kuuluukoOrganisaatioYosinPiiriin(parentOrganisaatiot, Some(oikeus.myontaja))
        })
        .filter(oikeus => tarkistaOpiskeluoikeudenKoulutusAsteenKuuluvuus(hakutoive, oikeudet, oikeus))
        .map(muodostaYosPaatettavaOpiskeluOikeus)
      LOGGER.info(s"Oikeuksista löytyi ${paatettavatOikeudet.size} kappaletta päätettävää oikeutta hakijalle $oppilasNro")
      Right(paatettavatOikeudet)
    } catch {
      case e: Exception =>
        LOGGER.error(s"Virhe hakiessa päätettäviä opiskeluoikeuksia hakijalle $oppilasNro", e)
        Left(new RuntimeException(s"Virhe hakiessa päätettäviä opiskeluoikeuksia hakijalle $oppilasNro", e))
    }
  }

  private def tarkistaOpiskeluoikeudenKoulutusAsteenKuuluvuus(hakutoive: YosHakutoive, oikeudet: Set[KKOpiskeluoikeus], oikeus: KKOpiskeluoikeus) = {
    var oikeudenAste = getKoulutusAsteOpiskeluOikeudelle(oikeus)
    //tarkistetaan onko ylemmällä asteella linkki alemmalle asteelle ja käytetään sitä
    if (oikeudenAste.equals(YLEMMAT_ASTEET) && oikeus.liittyvaOpiskeluoikeusAvain.isDefined) {
      oikeudenAste = oikeudet.find(o => o.virtaTunniste == oikeus.liittyvaOpiskeluoikeusAvain.get)
        .map(getKoulutusAsteOpiskeluOikeudelle)
        .filter(o => o.equals(ALEMMAT_ASTEET)).getOrElse(oikeudenAste)
      if (oikeudenAste.equals(ALEMMAT_ASTEET)) {
        LOGGER.info(s"Opiskeluoikeudelle ${oikeus.virtaTunniste} löytyi linkki alemmalle asteelle. Käytetään alempaa astetta koulutusasteen YOS-vertailussa")
      }
    }
    val kuuluu = YosPredicate.kuuluukoOpiskeluOikeusYosinPiiriinKoulutusAsteenMukaan(hakutoive.koulutusAste, oikeudenAste)
    if (kuuluu) {
      LOGGER.info(s"Opiskeluoikeus ${oikeus.virtaTunniste} kuuluu päätettäviin koulutusasteen $oikeudenAste mukaan. Hakutoiveen koulutusaste oli ${hakutoive.koulutusAste}")
    } else {
      LOGGER.info(s"Opiskeluoikeus ${oikeus.virtaTunniste} ei kuulu päätettäviin koulutusasteen $oikeudenAste mukaan. Hakutoiveen koulutusaste oli ${hakutoive.koulutusAste}")
    }
    kuuluu
  }

  private def muodostaYosHakutoive(haku: KoutaHaku, hakutoive: KoutaHakukohde): YosHakutoive = {
    val organisaatioJaVanhemmat = List(hakutoive.tarjoaja) ++ organisaatioProvider.haeKaikkiOrganisaationParenttienOidit(hakutoive.tarjoaja)
    val koulutusAste = getKoulutusAsteHakutoiveelle(hakutoive)
    YosHakutoive(haku.isKorkeakouluHaku, hakutoive.johtaaTutkintoon.getOrElse(false), haku.isJatkotutkinto,
      haku.isErasmusMundusTaiKaksoistutkinto, organisaatioJaVanhemmat, koulutusAste)
  }
  
  private def getKoulutusAsteHakutoiveelle(hakutoive: KoutaHakukohde): YosKoulutusAsteLuokka = {
    val koodit = hakutoive.koulutusasteKoodiUrit.map(_.split("_").last)
    val containsAlempi: Boolean = koodit.exists(k => KOULUTUSASTE_ALEMMAT.contains(k))
    val containsYlempi: Boolean = koodit.exists(k => KOULUTUSASTE_YLEMMAT.contains(k))
    (containsAlempi, containsYlempi) match {
      case (_, true) =>
        YLEMMAT_JA_ALEMMAT_ASTEET
      case (true, false) =>
        ALEMMAT_ASTEET
      case _ =>
        EI_YOS_KOULUTUSASTETTA
    }
  }

  private def getKoulutusAsteOpiskeluOikeudelle(oikeus: KKOpiskeluoikeus): YosKoulutusAsteLuokka = {
    if (LAAKETIETEEN_LISENSIAATIT_KOULUTUSKOODIT.contains(oikeus.koulutusKoodi.getOrElse(""))) {
      ALEMMAT_ASTEET
    } else {
      val koodiAsteArvot = oikeus.koulutusKoodi
        .flatMap(k => koodistoProvider.haeKoodisto(KOULUTUS_KOODISTO).get(k))
        .map(k => koodistoProvider.haeAlakoodit(k.koodiUri))
        .getOrElse(List.empty)
        .filter(k => k.koodisto.koodistoUri.equals(YOS_KOULUTUSASTE_KOODISTO))
        .map(k => k.koodiArvo)

      val containsAlempi: Boolean = koodiAsteArvot.exists(k => KOULUTUSASTE_ALEMMAT.contains(k))
      val containsYlempi: Boolean = koodiAsteArvot.exists(k => KOULUTUSASTE_YLEMMAT.contains(k))

      (containsAlempi, containsYlempi) match {
        case (true, _) =>
          ALEMMAT_ASTEET
        case (false, true) =>
          YLEMMAT_ASTEET
        case _ =>
          EI_YOS_KOULUTUSASTETTA
      }
    }
  }

  private def getKoodiNimi(koodiArvo: Option[String], koodisto: String): Option[Kielistetty] = {
    koodiArvo.flatMap(arvo => koodistoProvider.haeKoodisto(koodisto).get(arvo).map(k => {
      val fi = k.metadata.find(_.kieli.equalsIgnoreCase("fi")).map(_.nimi)
      val sv = k.metadata.find(_.kieli.equalsIgnoreCase("sv")).map(_.nimi)
      val en = k.metadata.find(_.kieli.equalsIgnoreCase("en")).map(_.nimi)
      Kielistetty(fi, sv, en)
    }))
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
    val virtaOpiskeluOikeusId = VirtaOpiskeluoikeus.getVirtaOpiskeluoikeusId(oikeus.myontaja, oikeus.virtaTunniste)
    val supaNimi = getKoodiNimi(oikeus.koulutusKoodi, KOULUTUS_KOODISTO)
      .orElse(None)
    YosPaatettavaOpiskeluOikeus(virtaOpiskeluOikeusId, organisaatio, oikeus.nimi, supaNimi)
  }
}

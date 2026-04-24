package fi.oph.suorituspalvelu.yos

import fi.oph.suorituspalvelu.integration.TarjontaIntegration
import fi.oph.suorituspalvelu.integration.client.{KoutaHaku, KoutaHakukohde}
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import java.lang

@Service
class YosService(@Autowired tarjontaIntegration: TarjontaIntegration) {

  private val LOGGER = LoggerFactory.getLogger(classOf[YosService])

  def kuuluukoVastaanotettavaHakutoiveYossinpiiriin(hakuOid: String, hakukohdeOid: String): Boolean = {
    LOGGER.info(s"Tehdään päättely kuuluuko hakutoive $hakukohdeOid haussa $hakuOid YOS")
    val haku: Option[KoutaHaku] = tarjontaIntegration.getHaku(hakuOid)
    val hakutoive: KoutaHakukohde = tarjontaIntegration.getHakukohde(hakukohdeOid)
    val kuuluukoYOSsinPiiriin: Boolean = (haku, hakutoive) match {
      case (None, _) =>
        LOGGER.error(s"Hakua ei löydy oidilla: $hakuOid")
        throw lang.RuntimeException(s"Hakua ei löydy oidilla: $hakuOid")
      case (Some(_), null) =>
        LOGGER.error(s"Hakukohdetta ei löydy oidilla: $hakukohdeOid")
        throw lang.RuntimeException(s"Hakukohdetta ei löydy oidilla: $hakukohdeOid")
      case (Some(h), hk) =>
        val yosHakutoive = muodostaYosHakutoive(h, hakutoive)
        YosPredicate.kuuluukoHakutoiveYosinPiiriin(yosHakutoive)
    }
    LOGGER.info(s"Hakutoive $hakukohdeOid haussa $hakuOid ${if (kuuluukoYOSsinPiiriin) "kuuluu" else "ei kuulu"} YOS piiriin")
    kuuluukoYOSsinPiiriin
  }

  private def muodostaYosHakutoive(haku: KoutaHaku, hakutoive: KoutaHakukohde): YosHakutoive = {
    YosHakutoive(haku.isKorkeakouluHaku, hakutoive.johtaaTutkintoon.getOrElse(false), haku.isJatkotutkinto, haku.isErasmusMundusTaiKaksoistutkinto, "", "")
  }
}

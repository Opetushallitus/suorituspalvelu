package fi.oph.suorituspalvelu.parsing.koski

import fi.oph.suorituspalvelu.util.KoodistoProvider

object KoskiDataFilter {

  val KOODISTO_OPPIAINEET = "koskioppiaineetyleissivistava"

  def includePerusopetuksenOppiaine(osaSuoritus: OsaSuoritus, koodistoProvider: KoodistoProvider): Boolean = {
    val oppiaineKoodi = osaSuoritus.koulutusmoduuli.get.tunniste.get.koodiarvo

    val hasArviointi = osaSuoritus.arviointi.isDefined
    val isKoulukohtainen = !koodistoProvider.haeKoodisto(KOODISTO_OPPIAINEET).contains(oppiaineKoodi)
    val aineTiedossa = !"XX".equals(oppiaineKoodi)   
    val pakollinen = osaSuoritus.koulutusmoduuli.get.pakollinen.get
    val laajuusYli2vvk = osaSuoritus.koulutusmoduuli.get.laajuus.exists(l => l.arvo > 2)
    
    hasArviointi && !isKoulukohtainen && aineTiedossa && (pakollinen || laajuusYli2vvk)
  }
}

package fi.oph.suorituspalvelu.yos

object YosConstants {
  def YOS_KOULUTUSASTE_KOODISTO = "kansallinenkoulutusluokitus2016koulutusastetaso2"
  def KOULUTUSASTE_ALEMPI_KORKEAKOULU_TUTKINTO = "63"
  def KOULUTUSASTE_AMK = "62"
  def KOULUTUSASTE_YAMK = "71"
  def KOULUTUSASTE_YLEMPI_KORKEAKOULU_TUTKINTO = "72"

  def KOULUTUSASTE_ALEMMAT: Seq[String] = Seq(KOULUTUSASTE_ALEMPI_KORKEAKOULU_TUTKINTO, KOULUTUSASTE_AMK)
  def KOULUTUSASTE_YLEMMAT: Seq[String] = Seq(KOULUTUSASTE_YAMK, KOULUTUSASTE_YLEMPI_KORKEAKOULU_TUTKINTO)
  def KOULUTUSASTE_YLEMMAT_JA_ALEMMAT: Seq[String] = KOULUTUSASTE_ALEMMAT ++ KOULUTUSASTE_YLEMMAT
}

/**
 * https://virkailija.hahtuvaopintopolku.fi/koodisto-service/ui/koodisto/view/kansallinenkoulutusluokitus2016koulutusastetaso2/1
 *
 * https://wiki.eduuni.fi/pages/viewpage.action?pageId=634880440&spaceKey=KEIJO&title=Opiskeluoikeuden%2Bp%C3%A4%C3%A4tt%C3%A4minen%2B-%2Bm%C3%A4%C3%A4rittely
 *
 *
 * Koodivastineet päätettäville korkeakouluopiskeluoikeuksille
 *
 * ammattikorkeakoulututkinto = 62. Päätettävät opiskeluoikeudet 1)
 * Ylempi ammattikorkeakoulututkinto = 71. Päätettävät opiskeluoikeudet 2)
 * Yliopiston kanditutkinto (alempi korkeakoulututkinto) = 63. Päätettävät opiskeluoikeudet 1)
 * Yliopiston maisteritutkinto (alempi+ylempi korkekaoulututkinto) = 63 + 72(?). Päätettävät opiskeluoikeudet 2)
 * Yliopiston maisteritutkinto (yliopiston ylempi korkeakoulututkinto) = 72. Päätettävät opiskeluoikeudet 2)
 *
 *
 * 1) Alemmat korkeakoulututkinnot = 62, 63, 63 + 72(?)
 * 2) Alemmat + ylemmät korkeakoulututkinnot = 62, 63, 63 + 72(?), 72, 71
**/
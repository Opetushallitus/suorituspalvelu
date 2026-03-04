package fi.oph.suorituspalvelu.business

/**
 * Vakiot korkeakoulutukseen (KK) liittyville päättelyille.
 */
object KKConstants {

  /**
   * VIRTA-opiskeluoikeuksien tyyppikoodit (viittaa koodistoon "virtaopiskeluoikeudentyyppi")
   *
   */
  object VirtaOpiskeluoikeusTyyppi {
    val AMMATTIKORKEAKOULUTUTKINTO = "1"
    val ALEMPI_KORKEAKOULUTUTKINTO = "2"
    val YLEMPI_AMMATTIKORKEAKOULUTUTKINTO = "3"
    val YLEMPI_KORKEAKOULUTUTKINTO = "4"
    val LISENSIAATINTUTKINTO = "6"
    val TOHTORINTUTKINTO = "7"
    val KOTIMAINEN_OPISKELIJALIIKKUVUUS = "8"
    val AVOIMEN_OPINNOT = "13"

    /**
     * Tutkintoon johtavien opiskeluoikeustyyppien koodit
     */
    val TUTKINTOON_JOHTAVAT: Set[String] = Set(
      AMMATTIKORKEAKOULUTUTKINTO,
      ALEMPI_KORKEAKOULUTUTKINTO,
      YLEMPI_AMMATTIKORKEAKOULUTUTKINTO,
      YLEMPI_KORKEAKOULUTUTKINTO,
      LISENSIAATINTUTKINTO,
      TOHTORINTUTKINTO
    )
  }

  /**
   * Oppilaitostyyppikoodit (viittaa koodistoon "oppilaitostyyppi")
   */
  object Oppilaitostyyppi {
    val AMMATTIKORKEAKOULU = "oppilaitostyyppi_41"
    val YLIOPISTO = "oppilaitostyyppi_42"
    val KESAYLIOPISTO = "oppilaitostyyppi_66"
  }
}


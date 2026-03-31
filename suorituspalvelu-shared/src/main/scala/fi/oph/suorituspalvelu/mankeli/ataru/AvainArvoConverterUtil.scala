package fi.oph.suorituspalvelu.mankeli.ataru

import java.time.LocalDate

object AvainArvoConverterUtil {
  def getLukukausi(suorituspaiva: LocalDate) = {
    suorituspaiva.getMonthValue match {
      case m if m > 0 && m <= 6 => "1"
      case m if m > 6 && m <= 12 => "2"
    }
  }
}

package fi.oph.suorituspalvelu.parsing.ytr

import fi.oph.suorituspalvelu.business.{Koe, Koodi, Opiskeluoikeus, PoistettuOpiskeluoikeus, SuoritusTila, YOOpiskeluoikeus, YOTutkinto}
import org.slf4j.LoggerFactory

import java.util.UUID
import java.time.LocalDate

class YtrToSuoritusConverter()

object YtrToSuoritusConverter {

  val YO_KOKEET_KOODISTO = "yokokeet"
  val YO_ARVOSANAT_KOODISTO = "koskiyoarvosanat"

  def parseTutkintokerta(tutkintoKerta: String): LocalDate =
    LocalDate.parse(tutkintoKerta.substring(0, 4) + (if(tutkintoKerta.endsWith("K")) "-06-01" else "-12-21"))

  def toSuoritus(student: Student): Opiskeluoikeus = {
    val yoTutkinto = {
      if (student.language.isEmpty) {
        // jos kieli on tyhjä, henkilö ei pitäisi vielä olla suorittanut mitään
        if (!student.exams.isEmpty)
          throw new RuntimeException("Henkilöllä on YTR-tiedoissa kokeita muttei kieltä")
        None
      } else {
        Some(YOTutkinto(
          UUID.randomUUID(),
          Koodi(student.language.get.toUpperCase(), "kieli", Some(1)),
          if (student.graduationPeriod.isDefined) then SuoritusTila.VALMIS else SuoritusTila.KESKEN,
          student.graduationDate.map(d => LocalDate.parse(d)),
          student.exams.map(koe => Koe(
            UUID.randomUUID(),
            Koodi(koe.examId, YO_KOKEET_KOODISTO, Some(1)),
            parseTutkintokerta(koe.period),
            Koodi(koe.grade, YO_ARVOSANAT_KOODISTO, Some(1)),
            koe.points
          )).toSet
        ))
      }
    }

    YOOpiskeluoikeus(
      UUID.randomUUID(),
      yoTutkinto
    )
  }
}

package fi.oph.suorituspalvelu.parsing.ytr

import fi.oph.suorituspalvelu.business.{Koe, Koodi, YOOpiskeluoikeus, YOTutkinto, SuoritusTila}
import fi.oph.suorituspalvelu.integration.ytr.YtrDataForHenkilo

import java.util.UUID
import java.time.LocalDate

object YtrToSuoritusConverter {

  val YO_KOKEET_KOODISTO = "yokokeet"
  val YO_ARVOSANAT_KOODISTO = "koskiyoarvosanat"
  
  def parseTutkintokerta(tutkintoKerta: String): LocalDate =
    LocalDate.parse(tutkintoKerta.substring(0, 4) + (if(tutkintoKerta.endsWith("K")) "-06-01" else "-12-21"))

  def toSuoritus(student: Student): YOOpiskeluoikeus = {
    YOOpiskeluoikeus(
      UUID.randomUUID(),
      YOTutkinto(
        UUID.randomUUID(),
        Koodi(student.language.toUpperCase(), "kieli", Some(1)),
        if (student.graduationPeriod.isDefined) then SuoritusTila.VALMIS else SuoritusTila.KESKEN,
        student.graduationDate.map(d => LocalDate.parse(d)),
        student.exams.map(koe => Koe(
          UUID.randomUUID(),
          Koodi(koe.examId, YO_KOKEET_KOODISTO, Some(1)),
          parseTutkintokerta(koe.period),
          Koodi(koe.grade, YO_ARVOSANAT_KOODISTO, Some(1)),
          koe.points
        )).toSet
      )
    )
  }
}

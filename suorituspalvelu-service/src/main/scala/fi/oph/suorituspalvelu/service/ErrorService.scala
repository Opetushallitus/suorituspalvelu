package fi.oph.suorituspalvelu.service

import org.springframework.stereotype.Component

trait ErrorService {
  def reportErrors(jobName: String, errors: Seq[(String, Option[Exception])]): Unit
}

@Component
class SupaErrorService extends ErrorService {

  def reportErrors(jobName: String, errors: Seq[(String, Option[Exception])]): Unit = {}
}

package fi.oph.suorituspalvelu.service

import fi.oph.suorituspalvelu.business.{KantaOperaatiot, Opiskeluoikeus, VersioEntiteetti}
import fi.oph.suorituspalvelu.integration.OnrIntegration
import fi.oph.suorituspalvelu.mankeli.{AvainArvoContainer, AvainArvoConverter, ValintaData}
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import slick.jdbc.JdbcBackend

import java.time.LocalDate
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

@Component
class ValintaDataService {

  @Autowired val kantaOperaatiot: KantaOperaatiot = null

  @Autowired val onrIntegration: OnrIntegration = null

  val LOG = LoggerFactory.getLogger(classOf[ValintaDataService])

  def fetchValintaDataForOppija(personOid: String, hakuOid: Option[String]): ValintaData = {
    //Todo, tarvitaan lopulta kaksi aikaleimaa:
    // -yksi tietojen haulle kannasta (laskennan alkamisen ajanhetki, mistä haetaan? Ohjausparametrit/Valintalaskenta/Koostepalvelu/muu, mikä?)
    // -toinen leikkuripäiväksi suoritusten vahvistuspäivämääriä vasten (haetaan ohjausparametreista, mutta ohjausparametria ei ole vielä lisätty)
    //Toistaiseksi käytetään jotain tulevaisuuden aikaleimaa molemmille, eli käytetään tuoreimpia versioita ja kaikki suoritukset kelpaavat.

    val vahvistettuViimeistaan = LocalDate.parse("2055-01-01")

    val allOids = Await.result(onrIntegration.getAliasesForPersonOids(Set(personOid)), 10.seconds).allOids
    LOG.info(s"Saatiin oppijalle $personOid aliakset: $allOids")
    val opiskeluoikeudet = allOids.flatMap(oid => kantaOperaatiot.haeSuoritukset(oid).values.toSet.flatten)

    LOG.info(s"Muodostetaan avain-arvot henkilölle $personOid, ${opiskeluoikeudet.size} opiskeluoikeutta ja vahvistettu viimeistään $vahvistettuViimeistaan")
    AvainArvoConverter.convertOpiskeluoikeudet(personOid, opiskeluoikeudet.toSeq, vahvistettuViimeistaan)

  }

}

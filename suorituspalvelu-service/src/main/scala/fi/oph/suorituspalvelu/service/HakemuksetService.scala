package fi.oph.suorituspalvelu.service

import fi.oph.suorituspalvelu.integration.TarjontaIntegration
import fi.oph.suorituspalvelu.integration.client.{AtaruHakemusBaseFields, HakemuspalveluClient}
import fi.oph.suorituspalvelu.jobs.SupaScheduler
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import java.time.Instant
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

@Component
class HakemuksetService(supaScheduler: SupaScheduler, hakemusPalveluClient: HakemuspalveluClient, tarjontaIntegration: TarjontaIntegration,
                        virtaService: VirtaService, ytrService: YTRService, koskiService: KoskiService,
                        @Value("${integrations.ataru.cron}") cron: String,
                        @Value("${integrations.ataru.ytr-refresh-enabled}") ytrRefreshEnabled: Boolean) {

  val LOG = LoggerFactory.getLogger(classOf[HakemuksetService])

  // Pollataan atarusta muuttuneita hakemuksia
  supaScheduler.scheduleJob("ataru-poll-muuttuneet", (ctx, data) => {
    val start = Instant.now()
    val prevStart = Option.apply(data).map(Instant.parse(_))
    if (prevStart.isDefined) // tyhjä tarkoittaa ettei taskia ajettu koskaan tässä ympäristössä
      try
        val hakemukset = Await.result(hakemusPalveluClient.getMuuttuneetHakemukset(prevStart.get.minusSeconds(60)), 1.minutes)
        LOG.info(s"Saatiin ${hakemukset.size} muuttunutta hakemusta")
        prosessoiMuuttuneetHakemukset(hakemukset)
        start.toString
      catch
        case e: Exception =>
          LOG.error("Muuttuneiden hakemustietojen pollaus epäonnistui", e)
          prevStart.map(_.toString).orNull
    else
      start.toString
  }, cron)

  def prosessoiMuuttuneetHakemukset(hakemukset: Seq[AtaruHakemusBaseFields]): Unit =
    if(hakemukset.nonEmpty)
      // KOSKI-tiedot haetaan aina
      val henkilot = hakemukset.map(_.personOid).toSet
      koskiService.startRefreshForHenkilot(henkilot)
      virtaService.startRefreshForHenkilot(henkilot)
      if (ytrRefreshEnabled) ytrService.startRefreshForHenkilot(henkilot)
}

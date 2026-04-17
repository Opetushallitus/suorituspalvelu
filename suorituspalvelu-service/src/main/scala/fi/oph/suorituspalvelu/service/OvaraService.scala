package fi.oph.suorituspalvelu.service

import fi.oph.suorituspalvelu.business.KantaOperaatiot
import fi.oph.suorituspalvelu.integration.{OnrIntegration, TarjontaIntegration}
import fi.oph.suorituspalvelu.integration.client.{AtaruValintalaskentaHakemus, HakemuspalveluClient, KoutaHaku, SiirtotiedostoClient}
import fi.oph.suorituspalvelu.mankeli.{HakemuksenHarkinnanvaraisuus, HakutoiveenHarkinnanvaraisuus, HarkinnanvaraisuusService}
import fi.oph.suorituspalvelu.parsing.OpiskeluoikeusParsingService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import java.time.LocalDate
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

//Avain-arvot (toinen aste)
//Ensikertalaisuudet (kk-haut)
//Harkinnanvaraisuudet (toisen asteen yhteishaku)

case class OvaraValintaData(
  personOid: String,
  hakemusOid: String,
  hakuOid: String,
  avainArvot: Map[String, String]
)

case class OvaraHarkinnanvaraisuusData(
                                        henkiloOid: String,
                                        hakemusOid: String,
                                        hakuOid: String,
                                        harkinnanvaraisuudet: List[HakutoiveenHarkinnanvaraisuus]
)

@Component
class OvaraService {

  private val LOG = LoggerFactory.getLogger(classOf[OvaraService])

  private val HAKEMUS_BATCH_SIZE = 500

  @Autowired val kantaOperaatiot: KantaOperaatiot = null

  @Autowired val siirtotiedostoClient: SiirtotiedostoClient = null

  @Autowired val tarjontaIntegration: TarjontaIntegration = null

  @Autowired val opiskeluoikeusParsingService: OpiskeluoikeusParsingService = null

  @Autowired val onrIntegration: OnrIntegration = null

  @Autowired val hakemuspalveluClient: HakemuspalveluClient = null

  @Autowired val harkinnanvaraisuusService: HarkinnanvaraisuusService = null

  @Autowired val valintaDataService: ValintaDataService = null


  case class KasittelyTila(valintaDataTiedostoNumero: Int,
                           harkinnanvaraisuusTiedostoNumero: Int,
                           epaonnistuneet: Seq[(String, Exception)])

  def kasitteleHaku(haku: KoutaHaku): KasittelyTila = {
    val executionId = s"${haku.oid}-${LocalDate.now()}"
    LOG.info(s"($executionId) Aloitetaan haun ${haku.oid} käsittely")

    val resultForHaku = for {
      hakijat <- hakemuspalveluClient.getHaunHakijat(haku.oid)
      kaikkiAliaksetMap <- onrIntegration.getAliasesForPersonOids(hakijat.flatMap(_.personOid).toSet)
      hakemusOids = hakijat.map(_.oid).toSet
      finalTila: KasittelyTila <- hakemusOids.toSeq.grouped(HAKEMUS_BATCH_SIZE).toSeq
        .foldLeft(Future.successful(KasittelyTila(1, 1, Seq.empty))) { (tilaF, batch) =>
          for {
            tila <- tilaF
            hakemukset <- valintaDataService.fetchValintalaskentaHakemukset(None, batch.toSet, haku.isToisenAsteenHaku())
          } yield {
            LOG.info(s"($executionId) Käsitellään erä ${hakemukset.size} hakemusta")
            val (valintaDataFailures, valintaDatat) = hakemukset.map(hakemus =>
              val allOidsForPerson = kaikkiAliaksetMap.allOidsByQueriedOids.getOrElse(hakemus.personOid, Set(hakemus.personOid))
              try {
                Right(valintaDataService.doAvainArvoConversions(None, haku, Some(hakemus), allOidsForPerson))
              } catch {
                case e: Exception =>
                  LOG.error(s"Virhe käsiteltäessä hakemuksen ${hakemus.hakemusOid} valinta-dataa", e)
                  Left((hakemus.personOid, e))
              }
            ).partitionMap(identity)

            val valintaDataBatch = valintaDatat.flatMap { vd =>
              vd.hakemus.map(h => OvaraValintaData(
                personOid = vd.personOid,
                hakemusOid = h.hakemusOid,
                hakuOid = haku.oid,
                avainArvot = vd.kaikkiAvainArvotFull().map(a => (a.avain, a.arvo)).toMap
              ))
            }
            if (valintaDataBatch.nonEmpty) {
              LOG.info(s"($executionId) Tallennetaan muplautin-tiedosto ${tila.valintaDataTiedostoNumero}, ${valintaDataBatch.size} henkilöä")
              siirtotiedostoClient.saveSiirtotiedosto("valintadata", valintaDataBatch, executionId, tila.valintaDataTiedostoNumero)
            }

            val nextHarkinnanvaraisuusTiedostoNumero = if (haku.isToisenAsteenYhteisHaku()) {
              val harkinnanvaraisuusBatch = valintaDatat.flatMap { vd =>
                vd.harkinnanvaraisuudet.flatMap(hv =>
                  vd.hakemus.map(h => OvaraHarkinnanvaraisuusData(
                    henkiloOid = vd.personOid,
                    hakemusOid = h.hakemusOid,
                    hakuOid = haku.oid,
                    harkinnanvaraisuudet = hv.hakutoiveet
                  ))
                )
              }
              if (harkinnanvaraisuusBatch.nonEmpty) {
                LOG.info(s"($executionId) Tallennetaan harkinnanvaraisuus-tiedosto ${tila.harkinnanvaraisuusTiedostoNumero}, ${harkinnanvaraisuusBatch.size} henkilöä")
                siirtotiedostoClient.saveSiirtotiedosto("harkinnanvaraisuudet", harkinnanvaraisuusBatch, executionId, tila.harkinnanvaraisuusTiedostoNumero)
                tila.harkinnanvaraisuusTiedostoNumero + 1
              } else tila.harkinnanvaraisuusTiedostoNumero
            } else tila.harkinnanvaraisuusTiedostoNumero

            KasittelyTila(
              valintaDataTiedostoNumero = if (valintaDataBatch.nonEmpty) tila.valintaDataTiedostoNumero + 1 else tila.valintaDataTiedostoNumero,
              harkinnanvaraisuusTiedostoNumero = nextHarkinnanvaraisuusTiedostoNumero,
              epaonnistuneet = tila.epaonnistuneet ++ valintaDataFailures
            )
          }
        }
    } yield finalTila

    val finalTila = Await.result(resultForHaku, 60.minutes)

    if (finalTila.epaonnistuneet.nonEmpty) {
      LOG.error(s"($executionId) Epäonnistuneet henkilöt (${finalTila.epaonnistuneet.size} kpl): ${finalTila.epaonnistuneet.map(_._1).mkString(", ")}")
      finalTila.epaonnistuneet.foreach { case (personOid, e) => LOG.error(s"($executionId) Virhe henkilölle $personOid", e) }
    }
    LOG.info(s"($executionId) Haun ${haku.oid} käsittely valmis")
    finalTila
  }

  def muodostaPaivittaiset(): Unit = {
    val aktiivisetHaut = tarjontaIntegration.aktiivisetHaut()
    val toisenAsteenHaut = aktiivisetHaut.filter(_.isToisenAsteenHaku())
    // kk-haut (EnsikertalaisuusService): todo

    LOG.info(s"Käsitellään ${toisenAsteenHaut.size} toisen asteen hakua")

    toisenAsteenHaut.foreach { haku =>
      try {
        LOG.info(s"Käsitellään haku ${haku.oid}")
        kasitteleHaku(haku)
      } catch {
        case e: Exception =>
          LOG.error(s"Haun ${haku.oid} käsittely epäonnistui", e)
      }
    }
  }
}

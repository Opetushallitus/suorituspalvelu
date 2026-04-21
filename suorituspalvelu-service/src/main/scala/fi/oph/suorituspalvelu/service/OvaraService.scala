package fi.oph.suorituspalvelu.service

import fi.oph.suorituspalvelu.business.KantaOperaatiot
import fi.oph.suorituspalvelu.integration.{OnrIntegration, TarjontaIntegration}
import fi.oph.suorituspalvelu.integration.client.{
  AtaruValintalaskentaHakemus, HakemuspalveluClient, KoutaHaku, SiirtotiedostoClient
}
import fi.oph.suorituspalvelu.mankeli.{
  HakemuksenHarkinnanvaraisuus, HakutoiveenHarkinnanvaraisuus, HarkinnanvaraisuusService
}
import fi.oph.suorituspalvelu.parsing.OpiskeluoikeusParsingService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import java.time.LocalDate
import java.util.UUID
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

case class OvaraParams(
  executionId: String = UUID.randomUUID().toString,
  vainAktiiviset: Boolean = true,
  avainArvot: Boolean = true,
  harkinnanvaraisuudet: Boolean = true,
  ensikertalaisuudet: Boolean = false
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

  case class HaunKasittelyTila(
    hakuOid: String,
    valintaDataTiedostoNumero: Int,
    harkinnanvaraisuusTiedostoNumero: Int,
    onnistuneet: Int,
    epaonnistuneet: Seq[(String, Exception)]
  )

  private def hakemuksetToSiirtotiedosto(
    haku: KoutaHaku,
    hakemukset: Seq[AtaruValintalaskentaHakemus],
    kaikkiAliaksetMap: Map[String, Set[String]],
    params: OvaraParams,
    tila: HaunKasittelyTila
  ): HaunKasittelyTila = {
    val (valintaDataFailures, valintaDatat) = hakemukset.map(hakemus =>
      val allOidsForPerson = kaikkiAliaksetMap.getOrElse(hakemus.personOid, Set(hakemus.personOid))
      try {
        Right(valintaDataService.doAvainArvoConversions(None, haku, Some(hakemus), allOidsForPerson))
      } catch {
        case e: Exception =>
          LOG.error(s"Virhe käsiteltäessä hakemuksen ${hakemus.hakemusOid} valinta-dataa", e)
          Left((hakemus.personOid, e))
      }
    ).partitionMap(identity)

    val valintaDataBatch = valintaDatat.flatMap { vd =>
      vd.hakemus.map(h =>
        OvaraValintaData(
          personOid = vd.personOid,
          hakemusOid = h.hakemusOid,
          hakuOid = haku.oid,
          avainArvot = vd.kaikkiAvainArvotFull().map(a => (a.avain, a.arvo)).toMap
        )
      )
    }
    if (valintaDataBatch.nonEmpty) {
      LOG.info(
        s"(${params.executionId}) Tallennetaan haun ${haku.oid} avain-arvotiedosto ${tila.valintaDataTiedostoNumero}, ${valintaDataBatch.size} henkilöä"
      )
      siirtotiedostoClient.tallennaSiirtotiedosto(
        "valintadata",
        valintaDataBatch,
        params.executionId,
        tila.valintaDataTiedostoNumero
      )
    }

    val nextHarkinnanvaraisuusTiedostoNumero = if (haku.isToisenAsteenYhteisHaku()) {
      val harkinnanvaraisuusBatch = valintaDatat.flatMap { vd =>
        vd.harkinnanvaraisuudet.flatMap(hv =>
          vd.hakemus.map(h =>
            OvaraHarkinnanvaraisuusData(
              henkiloOid = vd.personOid,
              hakemusOid = h.hakemusOid,
              hakuOid = haku.oid,
              harkinnanvaraisuudet = hv.hakutoiveet
            )
          )
        )
      }
      if (harkinnanvaraisuusBatch.nonEmpty) {
        LOG.info(
          s"(${params.executionId}) Tallennetaan haun ${haku.oid} harkinnanvaraisuus-tiedosto ${tila.harkinnanvaraisuusTiedostoNumero}, ${harkinnanvaraisuusBatch.size} henkilöä"
        )
        siirtotiedostoClient.tallennaSiirtotiedosto(
          "harkinnanvaraisuudet",
          harkinnanvaraisuusBatch,
          params.executionId,
          tila.harkinnanvaraisuusTiedostoNumero
        )
        tila.harkinnanvaraisuusTiedostoNumero + 1
      } else tila.harkinnanvaraisuusTiedostoNumero
    } else tila.harkinnanvaraisuusTiedostoNumero

    val nextTila = tila.copy(
      valintaDataTiedostoNumero =
        if (valintaDataBatch.nonEmpty) tila.valintaDataTiedostoNumero + 1 else tila.valintaDataTiedostoNumero,
      harkinnanvaraisuusTiedostoNumero = nextHarkinnanvaraisuusTiedostoNumero,
      onnistuneet = tila.onnistuneet + valintaDatat.size,
      epaonnistuneet = tila.epaonnistuneet ++ valintaDataFailures
    )

    nextTila
  }

  def kasitteleHaku(haku: KoutaHaku, params: OvaraParams): Future[HaunKasittelyTila] = {
    val resultForHaku = for {
      hakijat <- hakemuspalveluClient.getHaunHakijat(haku.oid)
      kaikkiAliaksetMap <- onrIntegration.getAliasesForPersonOids(hakijat.flatMap(_.personOid).toSet)
      hakemusOids = hakijat.map(_.oid).toSet
      batches = hakemusOids.toSeq.grouped(HAKEMUS_BATCH_SIZE).toSeq
      batchCount = batches.size
      finalTila: HaunKasittelyTila <- batches.zipWithIndex
        .foldLeft(Future.successful(HaunKasittelyTila(haku.oid, 1, 1, 0, Seq.empty))) { (tilaF, batchWithIndex) =>
          val (hakemusOidBatch, batchIndex) = batchWithIndex
          tilaF.flatMap(tila => {
            val start = System.currentTimeMillis()
            LOG.info(
              s"(${params.executionId}) Käsitellään haun ${haku.oid} erä ${batchIndex + 1}/$batchCount, ${hakemusOidBatch.size} hakemusta"
            )
            for {
              hakemukset: Seq[AtaruValintalaskentaHakemus] <- valintaDataService.fetchValintalaskentaHakemukset(
                None,
                hakemusOidBatch.toSet,
                haku.isToisenAsteenHaku()
              )
            } yield {
              LOG.info(
                s"(${params.executionId}) Haun ${haku.oid} erän ${batchIndex + 1}/$batchCount ${hakemusOidBatch.size} hakemusta haettu " +
                  s"(kesto ${(System.currentTimeMillis() - start) / 1000}s). Tehdään muunnokset."
              )

              val nextTila: HaunKasittelyTila =
                hakemuksetToSiirtotiedosto(haku, hakemukset, kaikkiAliaksetMap.allOidsByQueriedOids, params, tila)

              val durationSeconds = (System.currentTimeMillis() - start) / 1000
              LOG.info(s"(${params.executionId}) Haun ${haku.oid} erä ${batchIndex + 1}/$batchCount valmis, kesto ${durationSeconds}s.")
              nextTila
            }
          })
        }
    } yield finalTila

    resultForHaku
  }

  def muodostaPaivittaisetHauille(params: OvaraParams): Unit = {
    val rinnakkaisuus = 4
    val semaphore = new java.util.concurrent.Semaphore(rinnakkaisuus)

    val muodostettavatHaut =
      if (params.vainAktiiviset) tarjontaIntegration.aktiivisetHaut() else tarjontaIntegration.kaikkiHaut()
    // kk-haut (EnsikertalaisuusService): todo

    val hakuCount = muodostettavatHaut.size
    LOG.info(s"(${params.executionId}) Käsitellään $hakuCount hakua")

    val allFutures = muodostettavatHaut.zipWithIndex.map { (haku, hakuIndex) =>
      semaphore.acquire()
      val hakuStart = System.currentTimeMillis()
      LOG.info(s"(${params.executionId}) Aloitetaan haku ${hakuIndex + 1}/$hakuCount: ${haku.oid}")
      val f = Future { kasitteleHaku(haku, params) }.flatten
        .map { finalTilaForHaku =>
          if (finalTilaForHaku.epaonnistuneet.nonEmpty) {
            LOG.error(
              s"(${params.executionId}) Epäonnistuneet henkilöt (${finalTilaForHaku.epaonnistuneet.size} kpl): ${finalTilaForHaku.epaonnistuneet.map(_._1).mkString(", ")}"
            )
            finalTilaForHaku.epaonnistuneet.foreach { case (personOid, e) =>
              LOG.error(s"(${params.executionId}) Virhe muodostettaessa haun ${finalTilaForHaku.hakuOid} siirtotiedostoa henkilölle $personOid", e)
            }
          }
          val durationSeconds = (System.currentTimeMillis() - hakuStart) / 1000
          LOG.info(s"(${params.executionId}) Haku ${hakuIndex + 1}/$hakuCount valmis, kesto ${durationSeconds}s, " +
            s"onnistuneita hakemuksia ${finalTilaForHaku.onnistuneet}, epäonnistuneita: ${finalTilaForHaku.epaonnistuneet.size}: ${haku.oid}")
          None
        }
        .recover { case e: Exception =>
          val durationSeconds = (System.currentTimeMillis() - hakuStart) / 1000
          LOG.error(s"(${params.executionId}) Haku ${hakuIndex + 1}/$hakuCount epäonnistui, kesto ${durationSeconds}s: ${haku.oid}", e)
          Some(haku.oid)
        }
      f.onComplete(_ => semaphore.release())
      f
    }

    val epaonnistuneetHaut = Await.result(Future.sequence(allFutures), 120.minutes).flatten

    if (epaonnistuneetHaut.nonEmpty) {
      LOG.error(s"(${params.executionId}) Epäonnistuneet haut (${epaonnistuneetHaut.size}/$hakuCount): ${epaonnistuneetHaut.mkString(", ")}")
    }
    LOG.info(s"(${params.executionId}) Valmista!")
  }
}

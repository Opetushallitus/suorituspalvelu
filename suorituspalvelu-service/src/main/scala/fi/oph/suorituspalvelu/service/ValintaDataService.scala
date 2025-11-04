package fi.oph.suorituspalvelu.service

import fi.oph.suorituspalvelu.business.{AvainArvoYliajo, KantaOperaatiot, Opiskeluoikeus}
import fi.oph.suorituspalvelu.integration.{OnrIntegration, TarjontaIntegration}
import fi.oph.suorituspalvelu.integration.client.{AtaruValintalaskentaHakemus, HakemuspalveluClient}
import fi.oph.suorituspalvelu.mankeli.{AvainArvoConstants, AvainArvoContainer, AvainArvoConverter, AvainArvoConverterResults, ConvertedAtaruHakemus, ValintalaskentaHakutoive}
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import java.time.LocalDate
import java.util.concurrent.Executors
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.DurationInt

case class AvainArvoMetadata(selitteet: Seq[String],
                             duplikaatti: Boolean,
                             arvoEnnenYliajoa: Option[String],
                             yliajo: Option[AvainArvoYliajo],
                             arvoOnHakemukselta: Boolean)
case class CombinedAvainArvoContainer(avain: String, arvo: String, metadata: AvainArvoMetadata)

case class ValintalaskentaHakemus(hakuOid: String,
                                  hakemusOid: String,
                                  hakukohteet: List[ValintalaskentaHakutoive],
                                  hakijaOid: String,
                                  etunimi: String = "", //Todo, voiko pudottaa pois?
                                  sukunimi: String = "", //Todo, voiko pudottaa pois?
                                  koskiOpiskeluoikeudetJson: String, //Tässä vaiheessa lähinnä placeholder. Ensivaiheessa haetaan erikseen Koostepalvelussa/Valintalaskennassa. Tulevaisuudessa kuitenkin voidaan toimittaa suoraan Supasta.
                                  avaimet: Seq[AvainArvo],
                                  avainMetatiedotDTO: Seq[AvainMetatiedotDTO] = Seq.empty //Lisätään nämä myöhemmässä vaiheessa, tai yhdistetään avain-arvoihin (vaatii muutoksia valintaperusteisiin jos yhdistetään)
)

case class ValintaData(personOid: String, paatellytAvainArvot: Seq[CombinedAvainArvoContainer], hakemus: Option[ConvertedAtaruHakemus], opiskeluoikeudet: Seq[Opiskeluoikeus], vahvistettuViimeistaan: String, laskennanAlkaminen: String) {
  def getAvainArvoMap: Map[String, String] = paatellytAvainArvot.map(a => (a.avain, a.arvo)).toMap

  def hakemuksenAvainArvot = hakemus.map(_.avainArvot).getOrElse(Seq.empty).map(aa => CombinedAvainArvoContainer(aa.avain, aa.arvo, AvainArvoMetadata(aa.selitteet, duplikaatti = false, None, None, arvoOnHakemukselta = true)))

  def kaikkiAvainArvotFull(): Seq[CombinedAvainArvoContainer] = paatellytAvainArvot ++ hakemuksenAvainArvot

  def kaikkiAvainArvotMinimal(): Seq[AvainArvo] = kaikkiAvainArvotFull().map(aac => AvainArvo(aac.avain, aac.arvo))
}

case class HakutoiveenTiedot()
case class AvainArvo(avain: String, arvo: String)
case class AvainMetatiedotDTO()

@Component
class ValintaDataService {

  @Autowired val kantaOperaatiot: KantaOperaatiot = null

  @Autowired val onrIntegration: OnrIntegration = null

  @Autowired val hakemuspalveluClient: HakemuspalveluClient = null

  @Autowired val tarjontaIntegration: TarjontaIntegration = null

  val LOG = LoggerFactory.getLogger(classOf[ValintaDataService])

  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(6))

  def fetchOverridesForOppijaAliases(allOidsForSinglePerson: Set[String], hakuOid: String): Set[AvainArvoYliajo] = {
    allOidsForSinglePerson.flatMap(personOid => kantaOperaatiot.haeOppijanYliajot(personOid, hakuOid))
  }

  def fetchOverridesForOppija(personOid: String, hakuOid: String): Seq[AvainArvoYliajo] = {
    kantaOperaatiot.haeOppijanYliajot(personOid, hakuOid)
  }

  def saveOverridesForOppija(personOid: String, hakuOid: String, overrides: Seq[AvainArvoYliajo]): Unit = {
    kantaOperaatiot.tallennaYliajot(overrides)
  }

  def expandWithAvainAliases(originalContainers: Seq[CombinedAvainArvoContainer]): Seq[CombinedAvainArvoContainer] = {
    val aliasContainers: Seq[CombinedAvainArvoContainer] = originalContainers.flatMap(oc => {
      val avainAliakset = AvainArvoConstants.avainToRinnakkaisAvaimet.getOrElse(oc.avain, Set.empty)
      avainAliakset.map(avainAlias => oc.copy(avain = avainAlias, metadata = oc.metadata.copy(duplikaatti = true)))
    })
    originalContainers ++ aliasContainers
  }

  def combineBaseAvainArvotWithYliajot(baseResults: AvainArvoConverterResults, yliajot: Set[AvainArvoYliajo]): Set[CombinedAvainArvoContainer] = {
    val yliajotMap: Map[String, AvainArvoYliajo] = yliajot.map(y => (y.avain, y)).toMap
    LOG.info(s"Käsitellään yhteensä ${yliajotMap.size} yliajoa (${yliajotMap.keySet.mkString(",")}) henkilölle ${baseResults.personOid}")

    //Tehdään mahdolliset yliajot sellaisille arvoille, joille on jo tuloksia
    val tuloksetYliajoilla: Set[CombinedAvainArvoContainer] =
      baseResults.paatellytArvot.map((baseContainer: AvainArvoContainer) => {
        val yliajo: Option[AvainArvoYliajo] = yliajotMap.get(baseContainer.avain)
        yliajo match {
          case None =>
            val metadata = AvainArvoMetadata(baseContainer.selitteet, duplikaatti = false, None, None, arvoOnHakemukselta = false)
            CombinedAvainArvoContainer(baseContainer.avain, baseContainer.arvo, metadata)
          case Some(yliajo) =>
            val metadata = AvainArvoMetadata(baseContainer.selitteet, duplikaatti = false, Some(baseContainer.arvo), Some(yliajo), arvoOnHakemukselta = false)
            CombinedAvainArvoContainer(baseContainer.avain, yliajo.arvo, metadata)
        }
      })

    //Lisätään synteettiset tulokset sellaisille yliajoille, joille ei ollut valmista tulosta yliajettavaksi.
    val tuloksettomatYliajot: Iterable[AvainArvoYliajo] = yliajotMap.filter(yliajo => !tuloksetYliajoilla.exists(_.avain.equals(yliajo._2.avain))).values
    val synteettisetTulokset: Set[CombinedAvainArvoContainer] = tuloksettomatYliajot.map(yliajo => {
      CombinedAvainArvoContainer(yliajo.avain, yliajo.arvo, AvainArvoMetadata(Seq.empty, duplikaatti = false, None, Some(yliajo), arvoOnHakemukselta = false))
    }).toSet

    tuloksetYliajoilla ++ synteettisetTulokset

  }

  def haeOppijanJaAliastenOpiskeluoikeudet(allOids: Set[String]): Seq[Opiskeluoikeus] = {
    allOids.flatMap(oid => kantaOperaatiot.haeSuoritukset(oid).values.flatten).toSeq
  }

  def doAvainArvoConversions(personOid: Option[String], hakuOid: Option[String], hakemus: Option[AtaruValintalaskentaHakemus]): ValintaData = {
    val usePersonOid = personOid.getOrElse(hakemus.map(_.personOid).get) //personOid tarvitaan tai kaadutaan
    val allOidsForPerson = Await.result(onrIntegration.getAliasesForPersonOids(Set(usePersonOid)), 10.seconds).allOids
    //Todo, aikaleimat haun ohjausparametreista ja defaultit tulevaisuuteen jos hakua ei määritelty
    val vahvistettuViimeistaan = LocalDate.parse("2055-01-01")
    val kaikkiOpiskeluoikeudet = haeOppijanJaAliastenOpiskeluoikeudet(allOidsForPerson)
    val rawResults = AvainArvoConverter.convertOpiskeluoikeudet(usePersonOid, hakemus, kaikkiOpiskeluoikeudet, vahvistettuViimeistaan)

    val yliajot = hakuOid.map(hakuOid => fetchOverridesForOppijaAliases(allOidsForPerson, hakuOid)).getOrElse(Set.empty)
    val combinedWithYliajot = combineBaseAvainArvotWithYliajot(rawResults, yliajot)
    val withAliases = expandWithAvainAliases(combinedWithYliajot.toSeq)
    ValintaData(usePersonOid, withAliases, rawResults.convertedHakemus, kaikkiOpiskeluoikeudet, vahvistettuViimeistaan.toString, LocalDate.now().toString)
  }

  //Tämä palauttaa tiedot Valintalaskennan ymmärtämässä muodossa. Kts. fi.vm.sade.valintalaskenta.domain.dto.HakemusDTO
  def getValintalaskentaHakemukset(hakuOid: String, hakukohdeOid: Option[String], hakemusOids: Set[String]): Seq[ValintalaskentaHakemus] = {
    val haku = tarjontaIntegration.getHaku(hakuOid)
    val isToisenAsteenHaku = haku.exists(_.isToisenAsteenHaku())
    val valintaDatat = for {
      hakemukset <- fetchValintalaskentaHakemukset(hakukohdeOid, hakemusOids, isToisenAsteenHaku)
    } yield {
      val convertedHakemukset: Seq[ValintaData] = hakemukset.map(hakemus => {
          doAvainArvoConversions(None, Some(hakuOid), Some(hakemus))
        })
      val valintalaskentaHakemukset = convertedHakemukset.map(vd => {
        ValintalaskentaHakemus(
          hakuOid = hakuOid,
          hakemusOid = vd.hakemus.map(_.hakemusOid).get,
          hakukohteet = vd.hakemus.map(_.hakutoiveet).getOrElse(List.empty),
          hakijaOid = vd.personOid,
          etunimi = "mock_etunimi",
          sukunimi = "mock_sukunimi",
          koskiOpiskeluoikeudetJson = "",
          avaimet = vd.kaikkiAvainArvotMinimal(),
          avainMetatiedotDTO = Seq.empty
        )
      })
      valintalaskentaHakemukset
    }
    Await.result(valintaDatat, 5.minutes)
  }

  def getValintaData(personOid: String, hakuOid: String): ValintaData = {
    LOG.info(s"Haetaan UI:n käyttöön avain-arvot, henkilö $personOid, haku $hakuOid")
    val haku = tarjontaIntegration.getHaku(hakuOid)
    val isToisenAsteenHaku = haku.exists(_.isToisenAsteenHaku())
    val resultF = for {
      hakemusOid: Option[String] <- selvitaHakijanHakemusOidHaussa(hakuOid, personOid)
      hakemus: Seq[AtaruValintalaskentaHakemus] <- fetchValintalaskentaHakemukset(None, Set.empty ++ hakemusOid, isToisenAsteenHaku)
    } yield {
      doAvainArvoConversions(Some(personOid), Some(hakuOid), hakemus.headOption)
    }
    Await.result(resultF, 1.minute)
  }

  //Toisen asteen hauille palautuu enemmän tietoja kuin kk-hauille. Tämä kerrotaan parametrina atarulle.
  def fetchValintalaskentaHakemukset(hakukohdeOid: Option[String], hakemusOids: Set[String], toisenAsteenHaku: Boolean): Future[Seq[AtaruValintalaskentaHakemus]] = {
    if (hakukohdeOid.isEmpty && hakemusOids.isEmpty)
      Future.successful(Seq.empty)
    else
      hakemuspalveluClient.getValintalaskentaHakemukset(hakukohdeOid, toisenAsteenHaku, hakemusOids)
  }

  def selvitaHakijanHakemusOidHaussa(hakuOid: String, personOid: String): Future[Option[String]] = {
    val hakemuksetF = hakemuspalveluClient.getHenkilonHakemustenTiedot(personOid).map(_.values.flatten.toSet)

    hakemuksetF.map(hakemukset => {
      hakemukset.filter(_.applicationSystemId.equals(hakuOid)) match {
        case tamanHaunHakemukset if tamanHaunHakemukset.isEmpty =>
          LOG.info(s"Henkilölle $personOid ei löytynyt hakemusta haussa $hakuOid.")
          None
        case tamanHaunHakemukset if tamanHaunHakemukset.size >= 2 =>
          LOG.warn(s"Henkilölle $personOid löytyi useita hakemuksia hausta $hakuOid: ${tamanHaunHakemukset.map(_.oid).mkString(",")}")
          None
        case tamanHaunHakemukset =>
          LOG.info(s"Henkilölle $personOid löytyi hakemus haussa $hakuOid: ${tamanHaunHakemukset.headOption.map(_.oid)}")
          tamanHaunHakemukset.headOption.map(_.oid)
      }
    })
  }
}

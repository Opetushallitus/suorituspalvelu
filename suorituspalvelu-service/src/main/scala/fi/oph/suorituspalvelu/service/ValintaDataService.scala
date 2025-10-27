package fi.oph.suorituspalvelu.service

import fi.oph.suorituspalvelu.business.{AvainArvoYliajo, KantaOperaatiot, Opiskeluoikeus}
import fi.oph.suorituspalvelu.integration.OnrIntegration
import fi.oph.suorituspalvelu.mankeli.{AvainArvoConstants, AvainArvoContainer, AvainArvoConverter, AvainArvoConverterResults}
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import java.time.LocalDate
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

case class AvainArvoMetadata(selitteet: Seq[String], duplikaatti: Boolean, arvoEnnenYliajoa: Option[String], yliajo: Option[AvainArvoYliajo])
case class CombinedAvainArvoContainer(avain: String, arvo: String, metadata: AvainArvoMetadata)

case class ValintaData(personOid: String, avainArvot: Seq[CombinedAvainArvoContainer], opiskeluoikeudet: Set[Opiskeluoikeus], vahvistettuViimeistaan: String, laskennanAlkaminen: String) {
  def getAvainArvoMap: Map[String, String] = avainArvot.map(a => (a.avain, a.arvo)).toMap
}

@Component
class ValintaDataService {

  @Autowired val kantaOperaatiot: KantaOperaatiot = null

  @Autowired val onrIntegration: OnrIntegration = null

  val LOG = LoggerFactory.getLogger(classOf[ValintaDataService])

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
      baseResults.containers.map((baseContainer: AvainArvoContainer) => {
        val yliajo: Option[AvainArvoYliajo] = yliajotMap.get(baseContainer.avain)
        yliajo match {
          case None =>
            val metadata = AvainArvoMetadata(baseContainer.selitteet, false, None, None)
            CombinedAvainArvoContainer(baseContainer.avain, baseContainer.arvo, metadata)
          case Some(yliajo) =>
            val metadata = AvainArvoMetadata(baseContainer.selitteet, false, Some(baseContainer.arvo), Some(yliajo))
            CombinedAvainArvoContainer(baseContainer.avain, yliajo.arvo, metadata)
        }
      })

    //Lisätään synteettiset tulokset sellaisille yliajoille, joille ei ollut valmista tulosta yliajettavaksi.
    val tuloksettomatYliajot: Iterable[AvainArvoYliajo] = yliajotMap.filter(yliajo => !tuloksetYliajoilla.exists(_.avain.equals(yliajo._2.avain))).values
    val synteettisetTulokset: Set[CombinedAvainArvoContainer] = tuloksettomatYliajot.map(yliajo => {
      CombinedAvainArvoContainer(yliajo.avain, yliajo.arvo, AvainArvoMetadata(Seq.empty, false, None, Some(yliajo)))
    }).toSet

    tuloksetYliajoilla ++ synteettisetTulokset

  }

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
    val converterResults = AvainArvoConverter.convertOpiskeluoikeudet(personOid, opiskeluoikeudet.toSeq, vahvistettuViimeistaan)
    val yliajot = hakuOid.map(hakuOid => fetchOverridesForOppija(personOid, hakuOid)).getOrElse(Seq.empty)

    val combinedWithYliajot: Set[CombinedAvainArvoContainer] = combineBaseAvainArvotWithYliajot(converterResults, yliajot.toSet)
    val withAliases = expandWithAvainAliases(combinedWithYliajot.toSeq)
    ValintaData(personOid, withAliases, opiskeluoikeudet, vahvistettuViimeistaan.toString, LocalDate.now().toString) //Todo, oikeat aikaleimat
  }
}

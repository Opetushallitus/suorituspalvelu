package fi.oph.suorituspalvelu.ovara

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import fi.oph.suorituspalvelu.integration.client.SiirtotiedostoClient
import fi.oph.suorituspalvelu.parsing.virta.{VirtaParser, VirtaToSuoritusConverter}
import org.junit.jupiter.api.Assertions

object OvaraLitistysTestUtil {

  // Sisältö JSON-puuna täsmälleen siinä muodossa, jossa SiirtotiedostoClient kirjoittaa sen siirtotiedostoon
  def siirtotiedostonJson(content: Seq[?]): JsonNode =
    new ObjectMapper().readTree(SiirtotiedostoClient.serialisoi(content))

  // Käärii opiskelijan sisällön (opiskeluoikeudet ja opintosuoritukset) Virran SOAP-vastaukseksi
  def virtaXml(opiskelijaAvain: String, opiskelijanSisalto: String): String =
    s"""<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
       |  <SOAP-ENV:Body>
       |    <virtaluku:OpiskelijanKaikkiTiedotResponse xmlns:virtaluku="http://tietovaranto.csc.fi/luku">
       |      <virta:Virta xmlns:virta="urn:mace:funet.fi:virta/2015/09/01">
       |        <virta:Opiskelija avain="$opiskelijaAvain">
       |$opiskelijanSisalto
       |        </virta:Opiskelija>
       |      </virta:Virta>
       |    </virtaluku:OpiskelijanKaikkiTiedotResponse>
       |  </SOAP-ENV:Body>
       |</SOAP-ENV:Envelope>""".stripMargin

  // Virta-XML -> business-entiteetit -> Ovara-entiteetit -> litistetyt KK-suoritukset
  def litistaVirtaXml(xml: String, meta: OvaraVersioMetadata): (Seq[OvaraKKOpiskeluoikeus], Seq[OvaraKKSynteettinenOpiskeluoikeus], Seq[OvaraLitistettyKKSuoritus]) = {
    val opiskeluoikeudet = VirtaToSuoritusConverter.toOpiskeluoikeudet(VirtaParser.parseVirtaOpiskelijat(xml))
    val ooJaMeta = opiskeluoikeudet.map(oo => (meta, oo))
    val kk = EntityToOvaraConverter.getKKOpiskeluoikeudet(ooJaMeta)
    val kkSynt = EntityToOvaraConverter.getKKSynteettisetOpiskeluoikeudet(ooJaMeta)
    (kk, kkSynt, EntityToOvaraConverter.litistaKKSuoritukset(kk, kkSynt))
  }

  // Rakenteelliset invariantit, joiden pitää päteä mille tahansa litistetylle suorituspuulle
  def tarkistaLitistyksenInvariantit(rivit: Seq[OvaraLitistettyKKSuoritus]): Unit = {
    Assertions.assertEquals(rivit.size, rivit.map(_.tunniste).distinct.size, "tunnisteet eivät ole uniikkeja")
    val tunnisteella = rivit.map(r => r.tunniste -> r).toMap
    val indeksi = rivit.map(_.tunniste).zipWithIndex.toMap
    rivit.foreach { r =>
      Assertions.assertEquals(r.juuriSuoritusPolku.lastOption, r.parentTunniste, s"${r.tunniste}: polun viimeinen ei ole parent")
      r.parentTunniste.foreach { p =>
        val parent = tunnisteella.getOrElse(p, Assertions.fail(s"${r.tunniste}: parentia $p ei löydy"))
        Assertions.assertTrue(parent.lapsiTunnisteet.contains(r.tunniste), s"${r.tunniste}: parent ei listaa lasta")
        Assertions.assertEquals(parent.juuriSuoritusPolku :+ p, r.juuriSuoritusPolku, s"${r.tunniste}: polku ei jatka parentin polkua")
        Assertions.assertEquals(parent.opiskeluoikeusTunniste, r.opiskeluoikeusTunniste, s"${r.tunniste}: eri opiskeluoikeus kuin parentilla")
        Assertions.assertTrue(indeksi(p) < indeksi(r.tunniste), s"${r.tunniste}: parent ei ole ennen lasta")
      }
      r.lapsiTunnisteet.foreach { l =>
        val lapsi = tunnisteella.getOrElse(l, Assertions.fail(s"${r.tunniste}: lasta $l ei löydy"))
        Assertions.assertEquals(Some(r.tunniste), lapsi.parentTunniste, s"$l: lapsi ei viittaa parentiin")
      }
    }
  }
}

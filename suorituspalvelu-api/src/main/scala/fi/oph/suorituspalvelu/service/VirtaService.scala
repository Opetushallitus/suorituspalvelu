package fi.oph.suorituspalvelu.service

import fi.oph.suorituspalvelu.integration.virta.VirtaClient
import fi.oph.suorituspalvelu.parsing.virta.{VirtaParser, VirtaToSuoritusConverter}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import java.io.ByteArrayInputStream
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

@Component
class VirtaService {

  final val TIMEOUT = 30.seconds

  @Autowired val virtaClient: VirtaClient = null

  def syncVirta(oppijaNumero: String): UUID =
    val virtaXMLs = Await.result(virtaClient.haeKaikkiTiedot(oppijaNumero, None), TIMEOUT)
    val parseroidut = virtaXMLs.map(r => VirtaParser.parseVirtaData(new ByteArrayInputStream(r.getBytes)))
    val konvertoidut = parseroidut.map(p => VirtaToSuoritusConverter.toSuoritukset(p).toSet).flatten
    UUID.randomUUID()

}

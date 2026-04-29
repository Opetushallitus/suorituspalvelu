package fi.oph.suorituspalvelu.yos

import fi.oph.suorituspalvelu.parsing.koski.Kielistetty

import java.util.UUID

case class YosOrganisaatio(oid: Option[String],
                           nimi: Kielistetty)

case class YosPaatettavaOpiskeluOikeus(tunniste: UUID,
                                       organisaatio: YosOrganisaatio,
                                       nimi: Option[Kielistetty],
                                       koulutusKoodi: Option[String]
                                      ) {
}

package fi.oph.suorituspalvelu.yos

import fi.oph.suorituspalvelu.parsing.koski.Kielistetty

import java.util.UUID

case class YosOrganisaatio(oid: Option[String],
                           nimi: Kielistetty)

case class YosPaatettavaOpiskeluOikeus(virtaOpiskeluOikeusId: String,
                                       organisaatio: YosOrganisaatio,
                                       virtaNimi: Option[Kielistetty],
                                       koulutusKoodi: Option[String]
                                      ) {
}

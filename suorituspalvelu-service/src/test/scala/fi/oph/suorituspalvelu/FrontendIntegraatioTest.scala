package fi.oph.suorituspalvelu

import org.junit.jupiter.api.*
import org.springframework.security.test.context.support.{WithAnonymousUser, WithMockUser}
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Integraatiotestit käyttöliittymän reitityksille.
 */
class FrontendIntegraatioTest extends BaseIntegraatioTesti {
  @WithAnonymousUser
  @Test def testFrontendAnonymous(): Unit =
    // Tuntematon käyttäjä ohjataan tunnistautumiseen
    mvc.perform(MockMvcRequestBuilders.get("/"))
      .andExpect(status().is3xxRedirection())
      .andExpect(MockMvcResultMatchers.redirectedUrlPattern("DUMMY_CAS_LOGIN*"))

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testFrontendStripIndex(): Unit =
    // Poistetaan index.html ja continue query-parametri
    mvc.perform(MockMvcRequestBuilders.get("/index.html"))
      .andExpect(status().is3xxRedirection()).andExpect(MockMvcResultMatchers.redirectedUrl("/"))

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testFrontendStripContinueParam(): Unit =
    // Poistetaan index.html ja continue query-parametri
    mvc.perform(MockMvcRequestBuilders.get("/?continue"))
      .andExpect(status().is3xxRedirection()).andExpect(MockMvcResultMatchers.redirectedUrl("/"))

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testFrontendStripIndexAndContinueParam(): Unit =
    // Poistetaan index.html ja continue query-parametri
    mvc.perform(MockMvcRequestBuilders.get("/index.html?myparam&continue"))
      .andExpect(status().is3xxRedirection()).andExpect(MockMvcResultMatchers.redirectedUrl("/?myparam"))

  @WithMockUser(value = "kayttaja", authorities = Array())
  @Test def testFrontendForwardToIndex(): Unit =
    // Tunnistettu käyttäjä -> näytetään käyttöliittymä
    mvc.perform(MockMvcRequestBuilders.get("/"))
      .andExpect(status().isOk).andExpect(MockMvcResultMatchers.forwardedUrl("/index.html"))

}

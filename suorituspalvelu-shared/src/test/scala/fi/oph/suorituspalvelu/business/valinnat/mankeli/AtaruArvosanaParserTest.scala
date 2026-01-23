package fi.oph.suorituspalvelu.business.valinnat.mankeli;

import fi.oph.suorituspalvelu.mankeli.ataru.{AtaruArvosanaParser, AvainArvoDTO}
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.{Assertions, Test, TestInstance}

import java.util.HashMap as JavaHashMap
import scala.jdk.CollectionConverters.*

@Test
@TestInstance(Lifecycle.PER_CLASS)
class AtaruArvosanaParserTest {

  private val HAKEMUS_OID = "1.2.246.562.11.00000000000000123456"

  // Helper method to create a Java Map of key-value pairs for testing
  private def createAvainArvoMap(entries: (String, String)*): JavaHashMap[String, AvainArvoDTO] = {
    val map = new JavaHashMap[String, AvainArvoDTO]()
    entries.foreach { case (key, value) =>
      map.put(key, new AvainArvoDTO(key, value))
    }
    map
  }

  @Test def testConvertAtaruAidinkieliValue(): Unit = {
    val testCases = Seq(
      ("suomi-aidinkielena", "FI"),
      ("suomi-toisena-kielena", "FI_2"),
      ("suomi-viittomakielisille", "FI_VK"),
      ("suomi-saamenkielisille", "FI_SE"),
      ("ruotsi-aidinkielena", "SV"),
      ("ruotsi-toisena-kielena", "SV_2"),
      ("ruotsi-viittomakielisille", "SV_VK"),
      ("saame-aidinkielena", "SE"),
      ("romani-aidinkielena", "RI"),
      ("viittomakieli-aidinkielena", "VK"),
      ("muu-oppilaan-aidinkieli", "XX"),
      ("tuntematon-arvo", "XX") // Default case
    )

    testCases.foreach { case (input, expected) =>
      val result = AtaruArvosanaParser.convertAtaruAidinkieliValue(input)
      Assertions.assertEquals(expected, result, s"Input '$input' should convert to '$expected'")
    }
  }

  @Test def testConvertValinnaisetKielet(): Unit = {
    val keyValues = createAvainArvoMap(
      "oppiaine-valinnainen-kieli_group0" -> "oppiaine-valinnainen-kieli-b2",
      "oppiaine-valinnainen-kieli_group1" -> "oppiaine-valinnainen-kieli-a2",
      "oppimaara-kieli-valinnainen-kieli_group0" -> "HE",
      "oppimaara-kieli-valinnainen-kieli_group1" -> "EN",
      "arvosana-valinnainen-kieli_group0" -> "arvosana-valinnainen-kieli-8",
      "arvosana-valinnainen-kieli_group1" -> "arvosana-valinnainen-kieli-6"
    )

    val result = AtaruArvosanaParser.convertValinnaisetKielet(keyValues, HAKEMUS_OID)

    // Should have 4 results (2 for B2, 2 for A2)
    Assertions.assertEquals(4, result.size())

    // Find entries for B2 and A2 languages
    val resultMap = result.asScala.map(dto => dto.getAvain -> dto.getArvo).toMap

    Assertions.assertTrue(resultMap.contains("PK_B2"), "Should contain PK_B2 key")
    Assertions.assertTrue(resultMap.contains("PK_B2_OPPIAINE"), "Should contain PK_B2_OPPIAINE key")
    Assertions.assertTrue(resultMap.contains("PK_A2"), "Should contain PK_A2 key")
    Assertions.assertTrue(resultMap.contains("PK_A2_OPPIAINE"), "Should contain PK_A2_OPPIAINE key")

    Assertions.assertEquals("8", resultMap("PK_B2"), "B2 grade should be 8")
    Assertions.assertEquals("HE", resultMap("PK_B2_OPPIAINE"), "B2 language should be HE")
    Assertions.assertEquals("6", resultMap("PK_A2"), "A2 grade should be 6")
    Assertions.assertEquals("EN", resultMap("PK_A2_OPPIAINE"), "A2 language should be EN")
  }

  @Test def testConvertValinnaisetKieletWithA1(): Unit = {
    val keyValues = createAvainArvoMap(
      "oppiaine-valinnainen-kieli_group0" -> "oppiaine-valinnainen-kieli-a1",
      "oppimaara-kieli-valinnainen-kieli_group0" -> "EN",
      "arvosana-valinnainen-kieli_group0" -> "arvosana-valinnainen-kieli-9"
    )

    val result = AtaruArvosanaParser.convertValinnaisetKielet(keyValues, HAKEMUS_OID)

    // Should convert A1 to A12
    val resultMap = result.asScala.map(dto => dto.getAvain -> dto.getArvo).toMap

    Assertions.assertTrue(resultMap.contains("PK_A12"), "Should convert A1 to PK_A12")
    Assertions.assertEquals("9", resultMap("PK_A12"), "A12 grade should be 9")
    Assertions.assertEquals("EN", resultMap("PK_A12_OPPIAINE"), "A12 language should be EN")
  }

  @Test def testConvertValinnaisetKieletWithMultipleA1(): Unit = {
    val keyValues = createAvainArvoMap(
      "oppiaine-valinnainen-kieli_group0" -> "oppiaine-valinnainen-kieli-a1",
      "oppiaine-valinnainen-kieli_group1" -> "oppiaine-valinnainen-kieli-a1",
      "oppimaara-kieli-valinnainen-kieli_group0" -> "EN",
      "oppimaara-kieli-valinnainen-kieli_group1" -> "SV",
      "arvosana-valinnainen-kieli_group0" -> "arvosana-valinnainen-kieli-9",
      "arvosana-valinnainen-kieli_group1" -> "arvosana-valinnainen-kieli-8"
    )

    val result = AtaruArvosanaParser.convertValinnaisetKielet(keyValues, HAKEMUS_OID)

    // Should convert to A12 and A13
    val resultList = result.asScala.map(dto => dto.getAvain -> dto.getArvo).toList
    val a12Entries = resultList.filter(_._1.startsWith("PK_A12"))
    val a13Entries = resultList.filter(_._1.startsWith("PK_A13"))

    Assertions.assertEquals(2, a12Entries.size, "Should have 2 entries for A12")
    Assertions.assertEquals(2, a13Entries.size, "Should have 2 entries for A13")

    // Verify correct values for each language
    Assertions.assertTrue(a12Entries.contains("PK_A12" -> "9"), "A12 grade should be 9")
    Assertions.assertTrue(a12Entries.contains("PK_A12_OPPIAINE" -> "EN"), "A12 language should be EN")

    Assertions.assertTrue(a13Entries.contains("PK_A13" -> "8"), "A13 grade should be 8")
    Assertions.assertTrue(a13Entries.contains("PK_A13_OPPIAINE" -> "SV"), "A13 language should be SV")
  }

  @Test def testConvertValinnaisetKieletWithMultipleA2B2(): Unit = {
    val keyValues = createAvainArvoMap(
      "oppiaine-valinnainen-kieli_group0" -> "oppiaine-valinnainen-kieli-a2",
      "oppiaine-valinnainen-kieli_group1" -> "oppiaine-valinnainen-kieli-a2",
      "oppimaara-kieli-valinnainen-kieli_group0" -> "EN",
      "oppimaara-kieli-valinnainen-kieli_group1" -> "SV",
      "arvosana-valinnainen-kieli_group0" -> "arvosana-valinnainen-kieli-9",
      "arvosana-valinnainen-kieli_group1" -> "arvosana-valinnainen-kieli-8"
    )

    val result = AtaruArvosanaParser.convertValinnaisetKielet(keyValues, HAKEMUS_OID)

    // First A2 should be PK_A2, second should be PK_A22
    val resultList: Seq[(String, String)] = result.asScala.map(dto => dto.getAvain -> dto.getArvo).toList
    val a2Entries = resultList.filter(r => r._1.equals("PK_A2") || r._1.equals("PK_A2_OPPIAINE"))
    val a22Entries = resultList.filter(r => r._1.equals("PK_A22") || r._1.equals("PK_A22_OPPIAINE"))

    Assertions.assertEquals(2, a2Entries.size, "Should have 2 entries for A2")
    Assertions.assertEquals(2, a22Entries.size, "Should have 2 entries for A22")

    // Verify correct values for each language
    Assertions.assertTrue(resultList.contains("PK_A2" -> "9"), "A2 grade should be 9")
    Assertions.assertTrue(resultList.contains("PK_A2_OPPIAINE" -> "EN"), "A2 language should be EN")

    Assertions.assertTrue(resultList.contains("PK_A22" -> "8"), "A22 grade should be 8")
    Assertions.assertTrue(resultList.contains("PK_A22_OPPIAINE" -> "SV"), "A22 language should be SV")
  }

  @Test def testConvertValinnaisetKieletWithEmptyValues(): Unit = {
    val keyValues = createAvainArvoMap(
      "oppiaine-valinnainen-kieli_group0" -> "oppiaine-valinnainen-kieli-b2",
      "oppiaine-valinnainen-kieli_group1" -> "", // Empty value
      "oppimaara-kieli-valinnainen-kieli_group0" -> "HE",
      "oppimaara-kieli-valinnainen-kieli_group1" -> "",
      "arvosana-valinnainen-kieli_group0" -> "arvosana-valinnainen-kieli-8",
      "arvosana-valinnainen-kieli_group1" -> ""
    )

    val result = AtaruArvosanaParser.convertValinnaisetKielet(keyValues, HAKEMUS_OID)

    // Should only process the valid entry (group0)
    Assertions.assertEquals(2, result.size(), "Should only have 2 entries from valid group")

    val resultMap = result.asScala.map(dto => dto.getAvain -> dto.getArvo).toMap
    Assertions.assertEquals("8", resultMap("PK_B2"), "B2 grade should be 8")
    Assertions.assertEquals("HE", resultMap("PK_B2_OPPIAINE"), "B2 language should be HE")
  }

  @Test def testConvertAtaruArvosanas(): Unit = {
    val keyValues = createAvainArvoMap(
      "arvosana-MA_group0" -> "arvosana-MA-9",
      "arvosana-A_group0" -> "arvosana-A-8", // Should be converted to AI
      "arvosana-HI_group0" -> "arvosana-HI-7",

      "oppiaine-valinnainen-kieli_group0" -> "oppiaine-valinnainen-kieli-b2",
      "oppimaara-kieli-valinnainen-kieli_group0" -> "HE",
      "arvosana-valinnainen-kieli_group0" -> "arvosana-valinnainen-kieli-8",

      "oppimaara-a_group0" -> "suomi-aidinkielena"
    )

    val result = AtaruArvosanaParser.convertAtaruArvosanas(keyValues, HAKEMUS_OID)

    // Should have all grades plus language info
    val resultMap = result.asScala.map(dto => dto.getAvain -> dto.getArvo).toMap

    // Check regular subject grades
    Assertions.assertEquals("9", resultMap("PK_MA"), "Math grade should be 9")
    Assertions.assertEquals("8", resultMap("PK_AI"), "AI grade should be 8 (converted from A)")
    Assertions.assertEquals("7", resultMap("PK_HI"), "History grade should be 7")

    // Check optional language
    Assertions.assertEquals("8", resultMap("PK_B2"), "B2 grade should be 8")
    Assertions.assertEquals("HE", resultMap("PK_B2_OPPIAINE"), "B2 language should be HE")

    // Check aidinkieli
    Assertions.assertEquals("FI", resultMap("PK_AI_OPPIAINE"), "Aidinkieli should be FI")
  }

  @Test def testConvertAtaruArvosanasWithGroupIndexes(): Unit = {
    val keyValues = createAvainArvoMap(
      "arvosana-MA_group0" -> "arvosana-MA-9", // Main grade
      "arvosana-MA_group1" -> "arvosana-MA-8", // Optional grade, should get _VAL1 suffix
      "arvosana-MA_group2" -> "arvosana-MA-7" // Optional grade, should get _VAL2 suffix
    )

    val result = AtaruArvosanaParser.convertAtaruArvosanas(keyValues, HAKEMUS_OID)

    // Should have all three math grades with correct keys
    val resultMap = result.asScala.map(dto => dto.getAvain -> dto.getArvo).toMap

    Assertions.assertEquals("9", resultMap("PK_MA"), "Primary math grade should be 9")
    Assertions.assertEquals("8", resultMap("PK_MA_VAL1"), "Second math grade should be 8")
    Assertions.assertEquals("7", resultMap("PK_MA_VAL2"), "Third math grade should be 7")
  }

  @Test def testConvertAtaruArvosanasWithInvalidGrade(): Unit = {
    val keyValues = createAvainArvoMap(
      "arvosana-MA_group0" -> "arvosana-MA-9",
      "arvosana-HI_group0" -> "arvosana-HI-invalid" // Invalid grade
    )

    val result = AtaruArvosanaParser.convertAtaruArvosanas(keyValues, HAKEMUS_OID)

    // Should include only the valid grade
    val resultMap = result.asScala.map(dto => dto.getAvain -> dto.getArvo).toMap

    Assertions.assertEquals(1, resultMap.size, "Should only include valid grades")
    Assertions.assertEquals("9", resultMap("PK_MA"), "Math grade should be 9")
    Assertions.assertFalse(resultMap.contains("PK_HI"), "Invalid history grade should be skipped")
  }

  @Test def testCombinedConversionWithAllFeatures(): Unit = {
    val keyValues = createAvainArvoMap(
      // Regular subjects
      "arvosana-MA_group0" -> "arvosana-MA-9",
      "arvosana-A_group0" -> "arvosana-A-8",

      // Optional languages
      "oppiaine-valinnainen-kieli_group0" -> "oppiaine-valinnainen-kieli-a1",
      "oppimaara-kieli-valinnainen-kieli_group0" -> "EN",
      "arvosana-valinnainen-kieli_group0" -> "arvosana-valinnainen-kieli-7",

      "oppimaara-a_group0" -> "ruotsi-aidinkielena",

      // Language subjects
      "oppimaara-kieli-B1_group0" -> "EN",
      "arvosana-B1_group0" -> "arvosana-B1-9"
    )

    val result = AtaruArvosanaParser.convertAtaruArvosanas(keyValues, HAKEMUS_OID)

    // Should contain all the expected keys with correct values
    val resultMap = result.asScala.map(dto => dto.getAvain -> dto.getArvo).toMap

    Assertions.assertEquals(7, resultMap.size, "Should have 7 result entries")

    // Regular subjects
    Assertions.assertEquals("9", resultMap("PK_MA"), "Math grade should be 9")
    Assertions.assertEquals("8", resultMap("PK_AI"), "AI grade should be 8")

    // A1 converted to A12
    Assertions.assertEquals("7", resultMap("PK_A12"), "A1 grade should be 7 and converted to A12")
    Assertions.assertEquals("EN", resultMap("PK_A12_OPPIAINE"), "A12 language should be EN")

    // Aidinkieli
    Assertions.assertEquals("SV", resultMap("PK_AI_OPPIAINE"), "Aidinkieli should be SV")

    // B1 language
    Assertions.assertEquals("9", resultMap("PK_B1"), "B1 grade should be 9")
    Assertions.assertEquals("EN", resultMap("PK_B1_OPPIAINE"), "B1 language should be EN")
  }
}

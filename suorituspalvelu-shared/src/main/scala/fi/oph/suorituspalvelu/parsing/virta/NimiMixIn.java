package fi.oph.suorituspalvelu.parsing.virta;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import scala.Option;

/**
 * Jackson ei ymmärrä Scala-case classiin laitettua JacksonXmlText-annotaatiota oikein, joten deserialisoidaan
 * java-luokka custom deserialisoijalla ja palautetaan custom-serialisoijassa tästä tehty scala-case class.
 */
public class NimiMixIn {

  @JacksonXmlText
  String value;

  @JacksonXmlProperty(localName = "kieli")
  Option<String> kieli;
}

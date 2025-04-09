package fi.oph.suorituspalvelu.util

import java.util.UUID

enum Mode:
  case LOCAL, TEST, PRODUCTION

object ConfigurationUtil {

  final val ENVIRONMENT_NAME_KEY = "ENVIRONMENT_NAME"

  lazy val environment = ConfigurationUtil.getConfigurationItem(ENVIRONMENT_NAME_KEY).get;

  lazy val opintopolkuDomain = sys.env.get("OPINTOPOLKU_DOMAIN") match
    case Some(domain) => domain
    case _ =>
      val environment = ConfigurationUtil.getConfigurationItem(ENVIRONMENT_NAME_KEY).get
      environment match
        case "localtest" => ConfigurationUtil.getConfigurationItem("LOCAL_OPINTOPOLKU_DOMAIN").get
        case "local" => ConfigurationUtil.getConfigurationItem("DEV_OPINTOPOLKU_DOMAIN").get
        case "pallero" => "testiopintopolku.fi"
        case "sade" => "opintopolku.fi"
        case _ => environment + "opintopolku.fi"

  def getConfigurationItem(key: String): Option[String] =
    sys.env.get(key).orElse(sys.props.get(key))

  def getMode(): Mode =
    getConfigurationItem("MODE").map(value => Mode.valueOf(value)).getOrElse(Mode.PRODUCTION)
}
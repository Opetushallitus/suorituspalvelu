package fi.oph.suorituspalvelu.util

object SecurityUtil {

  def onOikeus(): Boolean =
    true

  def onRekisterinpitaja(): Boolean = true
  
  def getIdentiteetti(): String =
    ""
}

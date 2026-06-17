package fi.oph.suorituspalvelu.util

import org.slf4j.{Logger, MDC}

/**
 * Luokka jonka avulla lokiviesteihin voi liittää metatietoa.
 */
object LogContext {

  final val PATH_KEY                  = "path"
  final val OPPIJANUMERO_KEY         = "liiteTunniste"
  final val IDENTITEETTI_KEY          = "identiteetti"
  final val LOGMETRICSOPERATION_KEY   = "logMetricsOperation"

  def apply[A](identiteetti: String = null, path: String = null, oppijaNumero: String = null, logMetricsOperation: String = null)(f: () => A): A =

    val prevPath = MDC.get(PATH_KEY)
    val prevOppijaNumero = MDC.get(OPPIJANUMERO_KEY)
    val prevIdentiteetti = MDC.get(IDENTITEETTI_KEY)
    val prevLogMetricsOperation = MDC.get(LOGMETRICSOPERATION_KEY)

    if(path!=null) MDC.put(PATH_KEY, path);
    if(oppijaNumero!=null) MDC.put(OPPIJANUMERO_KEY, oppijaNumero)
    if(identiteetti!=null) MDC.put(IDENTITEETTI_KEY, identiteetti)
    if(logMetricsOperation!=null) MDC.put(LOGMETRICSOPERATION_KEY, logMetricsOperation)

    try
      f()
    finally
      if(path!=null) MDC.put(PATH_KEY, prevPath)
      if(oppijaNumero!=null) MDC.put(OPPIJANUMERO_KEY, prevOppijaNumero)
      if(identiteetti!=null) MDC.put(IDENTITEETTI_KEY, prevIdentiteetti)
      if(logMetricsOperation!=null) MDC.put(LOGMETRICSOPERATION_KEY, prevLogMetricsOperation)
}

enum LogMetricsOperation:
  case KOSKIPOLLMUUTTUNEET
  case ATARUPOLLMUUTTUNEET
  case VIRTAREFRESHAKTIIVISET
  case YTRREFRESHAKTIIVISET

extension (logger: Logger) {

  // Operaatio jonka avulla lokitetaan tapahtumia AWS CloudWatchin lokimetriikoita varten. Tarkoitus on että varsinaiset
  // operaation tietoa lokittavat kutsut tehdään erikseen.
  def logOperation(operation: LogMetricsOperation): Unit = {
    LogContext(logMetricsOperation = operation.toString)(() =>
      logger.info(s"Operaatio ${operation.toString} valmis"))
  }
}
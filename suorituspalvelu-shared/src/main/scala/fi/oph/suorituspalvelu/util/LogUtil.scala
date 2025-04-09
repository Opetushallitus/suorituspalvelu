package fi.oph.suorituspalvelu.util

import org.slf4j.MDC

/**
 * Luokka jonka avulla lokiviesteihin voi liittää metatietoa.
 */
object LogContext {

  final val PATH_KEY                  = "path"
  final val OPPIJANUMERO_KEY         = "liiteTunniste"
  final val IDENTITEETTI_KEY          = "identiteetti"

  def apply[A](identiteetti: String = null, path: String = null, oppijaNumero: String = null)(f: () => A): A =

    val prevPath = MDC.get(PATH_KEY)
    val prevOppijaNumero = MDC.get(OPPIJANUMERO_KEY)
    val prevIdentiteetti = MDC.get(IDENTITEETTI_KEY)

    if(path!=null) MDC.put(PATH_KEY, path);
    if(oppijaNumero!=null) MDC.put(OPPIJANUMERO_KEY, oppijaNumero)
    if(identiteetti!=null) MDC.put(IDENTITEETTI_KEY, identiteetti)

    try
      f()
    finally
      if(path!=null) MDC.put(PATH_KEY, prevPath)
      if(oppijaNumero!=null) MDC.put(OPPIJANUMERO_KEY, prevOppijaNumero)
      if(identiteetti!=null) MDC.put(IDENTITEETTI_KEY, prevIdentiteetti)
}
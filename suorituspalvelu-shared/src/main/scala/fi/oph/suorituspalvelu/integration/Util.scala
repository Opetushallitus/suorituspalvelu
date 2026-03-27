package fi.oph.suorituspalvelu.integration

import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

object Util {

  private val LOG: Logger = LoggerFactory.getLogger(Util.getClass)

  /**
   * Geneerinen uudelleenyritysmekanismi eksponentiaalisella viiveellä. Uudelleenyritys tehdään vain, jos operation
   * palauttaa Future.failed. Viive tuplataan joka kerralla, ja uudelleenyrityksiä tehdään enintään retries-määrä.
   * Alkuviiveen voi määritellä initialDelayMillis-parametrillä.
   *
   * @param operation suoritettava operaatio joka tuottaa Future[T]
   * @param retries montako uudelleenyritystä on jäljellä
   * @param retryDelayMillis viive ennen uudelleenyritystä (tuplataan joka kerralla)
   * @param initialDelayMillis Viive ennen ensimmäistä yritystä
   * @param failMessage Virheviesti, joka näytetään kun operaatio epäonnistuu.
   * @return Future[T]
   */
  def retryWithBackoff[T](
    operation: => Future[T],
    retries: Int = 5,
    retryDelayMillis: Long = 5000,
    initialDelayMillis: Long = 0,
    failMessage: String = "Operaatio epäonnistui"
  )(using ec: ExecutionContext): Future[T] = {
    if (initialDelayMillis > 0) Thread.sleep(initialDelayMillis)
    operation.recoverWith {
      case e: Throwable =>
        if (retries > 0) {
          LOG.warn(s"$failMessage: ${e.getMessage}. Yritetään uudelleen ${retryDelayMillis}ms kuluttua ($retries yritystä jäljellä).")
          Thread.sleep(retryDelayMillis)
          retryWithBackoff(operation, retries - 1, retryDelayMillis * 2, 0, failMessage)
        } else {
          LOG.error(s"$failMessage: ${e.getMessage}. Ei uudelleenyrityksiä jäljellä.")
          Future.failed(e)
        }
    }
  }

  /**
   * Future.sequence-tyyppinen apumetodi, joka koostaa Iterator[Future[A]] tyyppisen futuuri-iteraattorin
   * Iterator[A] -tyyppiseksi iteraattoriksi siten että concurrency-parametrin määrittelemä määrä futuuri
   * -iteraattorin tuottamia futuureja on yhtä aikaa haettuna. Tämän käyttämisessä on järkeä vain kun
   * futuuri-iteraattori tuottaa futuureita laiskasti.
   *
   * @param futuresIterator iteraattori joka tuottaa Future[A] -tyyppisia futuureita
   * @param concurrency     kuinka monta futuuria yhtä aikaa suorituksessa
   * @param timeout         futuurien timeout
   *
   * @return iteraattoria tyyppiä A
   */
  def toIterator[A](iterator: Iterator[Future[A]], concurrency: Int, timeout: FiniteDuration) : Iterator[A] =
    def fillFutures(remaining: Int): Seq[Future[A]] =
      if(remaining==0 || !iterator.hasNext)
        Seq.empty
      else
        iterator.next() +: fillFutures(remaining-1)

    def nextState(state: (A, Seq[Future[A]])): Option[(A, Seq[Future[A]])] =
      state match
        case (_, futures) if futures.isEmpty => None
        case (_, futures) => Some(Await.result(futures.head, timeout), futures.tail ++ fillFutures(1))

    Iterator.unfold((null.asInstanceOf[A], fillFutures(concurrency-1)))(state => nextState(state).flatMap(s => Some(s._1, s)))
}

/**
 * Hiukan turvallisempi wrapperi Iterator-luokan ympärille jossa ei .size() -metodia tai muita ilmeisimpiä
 * footguneja.
 */
class SaferIterator[T](val underlying: Iterator[T]) {

  def hasNext: Boolean = underlying.hasNext
  def next(): T = underlying.next()

  def grouped(size: Int): SaferIterator[Seq[T]] = new SaferIterator(underlying.grouped(size))

  def map[U](f: T => U): SaferIterator[U] = new SaferIterator(underlying.map(f))

  def filter(p: T => Boolean): SaferIterator[T] = new SaferIterator(underlying.filter(p))

  def flatMap[U](f: T => IterableOnce[U] | SaferIterator[U]): SaferIterator[U] = new SaferIterator(underlying.flatMap(p => {
    f(p) match {
      case value if value.isInstanceOf[SaferIterator[_]] => value.asInstanceOf[SaferIterator[U]].underlying
      case value => value.asInstanceOf[IterableOnce[U]]
    }
  }))

  def foreach[U](f: T => U): Unit = underlying.foreach(f)

  def foldLeft[B](z: B)(op: (B, T) => B): B = underlying.foldLeft(z)(op)
}


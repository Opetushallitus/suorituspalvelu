package fi.oph.suorituspalvelu.jobs

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.{ExecutionComplete, ExecutionOperations, FailureHandler, Task, TaskDescriptor}
import com.github.kagkarlsson.scheduler.task.helper.{RecurringTask, Tasks}
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import fi.oph.suorituspalvelu.business.KantaOperaatiot
import fi.oph.suorituspalvelu.jobs.SupaScheduler.LOG
import fi.oph.suorituspalvelu.service.ErrorService
import org.slf4j.LoggerFactory

import java.time.{Duration, Instant}
import java.util.UUID
import scala.jdk.CollectionConverters.*

object SupaScheduler {
  val LOG = LoggerFactory.getLogger(classOf[SupaScheduler])
}

/**
 * Jobi-instanssille annettava konteksti jonka avulla voidaan päivittää jobin etenemistä
 */
trait SupaJobContext {

  /**
   * Päivittää jobin valmiusasteen
   *
   * @param progress  valmiusaste (välillä 0-1)
   */
  def updateProgress(progress: Double): Unit

  def reportError(message: String, exception: Option[Exception]): Unit

  def getJobId: String
}

val DUMMY_JOB_CTX = new SupaJobContext {
  override def updateProgress(progress: Double): Unit = {}
  override def reportError(message: String, exception: Option[Exception]): Unit = {}
  override def getJobId: String = "DUMMY JOB"  
}

trait SupaJob {
  def run(ctx: SupaJobContext, data: String): Unit
}

trait SupaScheduledJob {
  def run(ctx: SupaJobContext, data: String): String
}

case class SupaJobError(message: String, exception: Option[Exception])

/**
 * DB-Scheduler FailureHandler joka yrittää uudestaan määriteltyjen odotusaikojen jälkeen
 */
class SupaFailureHandler(retryWaits: Seq[Duration]) extends FailureHandler[String] {

  override def onFailure(executionComplete: ExecutionComplete, executionOperations: ExecutionOperations[String]): Unit = {
    val failures = executionComplete.getExecution.consecutiveFailures + 1
    val retryDurations = retryWaits.take(failures)
    if (failures > retryDurations.size) {
      LOG.error("Jobin {} suoritus on epäonnistunut {} kertaa, ei yritetä enää", executionComplete.getExecution().taskInstance, failures);
      executionOperations.stop();
    } else {
      val nextTry: Instant = executionComplete.getTimeDone.plus(retryDurations.last)
      LOG.debug("Jobin {} suoritus epäonnistui, yritetään uudestaan ajanhetkellä {}", executionComplete.getExecution.taskInstance, nextTry)
      executionOperations.reschedule(executionComplete, nextTry)
    }
  }
}

/**
 * Schedulering palauttama handle jobimäärittelyyn jonka avulla ajetaan jobi-instansseja
 */
class JobHandle(scheduler: SupaScheduler, name: String) {
  
  def run(data: String): UUID = scheduler.runJob(name, data, Instant.now())
}

/**
 * Ohut wrapperi DB-Schedulerille. Tällä on kaksi tavoitetta, a) tehdä apista hieman simppelimpi, ja b) mahdollistaa toteutuksen
 * vaihtaminen jos löytyy DB-Scheduleria parempi vaihtoehto.
 */
class SupaScheduler(threads: Int, pollingInterval: Duration, dataSource: javax.sql.DataSource, kantaOperaatiot: KantaOperaatiot, errorService: ErrorService) {
  
  var scheduler: Scheduler = null
  var taskDescriptors: Map[String, TaskDescriptor[String]] = Map.empty
  var tasks: List[Task[?]] = List.empty
  var recurringTasks: List[RecurringTask[String]] = List.empty

  /**
   * Rekisteröi erikseen ajettavan jobin
   *
   * @param name          jobin nimi
   * @param job           lambda (yleensä) joka sisältää jobin toiminnallisuuden
   * @param retryTimeouts uudelleenyritysten odotusajat
   * @return
   */
  def registerJob(name: String, job: SupaJob, retryTimeouts: Seq[Duration]): JobHandle = this.synchronized {
    val taskDescriptor = TaskDescriptor.of(name, classOf[String])
    taskDescriptors = taskDescriptors + (name -> TaskDescriptor.of(name, classOf[String]))
    tasks = tasks :+ Tasks.oneTime(taskDescriptor)
      .onFailure(SupaFailureHandler(retryTimeouts))
      .execute((instance, _) => {
        var errors: List[SupaJobError] = List.empty
        val ctx: SupaJobContext = new SupaJobContext {
          override def updateProgress(progress: Double): Unit = kantaOperaatiot.updateJobStatus(UUID.fromString(instance.getId), name, progress)
          override def reportError(message: String, exception: Option[Exception]): Unit = { errors = errors :+ SupaJobError(message, exception)}
          override def getJobId: String = instance.getId
        }
        ctx.updateProgress(0.0)
        job.run(ctx, instance.getData)
        ctx.updateProgress(1.0)
        errorService.reportErrors(name, errors.map(e => (e.message, e.exception)))
      })
    JobHandle(this, name)
  }

  private[jobs] def runJob(name: String, data: String, runAt: Instant): UUID =
    val taskDescriptor = taskDescriptors(name)
    val id = UUID.randomUUID()
    this.scheduler.schedule(taskDescriptor.instance(id.toString).data(data).build(), runAt)
    id

  /**
   * Rekisteröi toistuvasti ajettavan jobin
   *
   * @param name      jobin nimi
   * @param job       lambda (yleensä) joka sisältää jobin toiminnallisuuden
   * @param schedule  cron-aikataulu
   */
  def scheduleJob(name: String, job: SupaScheduledJob, schedule: String): Unit = this.synchronized {
    val task = Tasks.recurring(TaskDescriptor.of(name, classOf[String]), Schedules.cron(schedule))
      .executeStateful((instance, _) => {
        var errors: List[SupaJobError] = List.empty
        val jobId = UUID.randomUUID()
        val ctx: SupaJobContext = new SupaJobContext {
          override def updateProgress(progress: Double): Unit = kantaOperaatiot.updateJobStatus(jobId, name, progress)
          override def reportError(message: String, exception: Option[Exception]): Unit = this.synchronized { errors = errors :+ SupaJobError(message, exception) }
          override def getJobId: String = jobId.toString
        }
        ctx.updateProgress(0.0)
        val result = job.run(ctx, instance.getData)
        ctx.updateProgress(1.0)
        errorService.reportErrors(name, errors.map(e => (e.message, e.exception)))
        result
      })
    recurringTasks = recurringTasks :+ task
  }

  /**
   * Käynnistää schedulerin
   */
  def start(): Unit =
    this.scheduler = Scheduler
      .create(dataSource, tasks.asJava)
      .startTasks(recurringTasks.asJava)
      .threads(threads)
      .pollingInterval(pollingInterval)
      .enableImmediateExecution()
      .registerShutdownHook()
      .build();
    this.scheduler.start();

  /**
   * Sammuttaa schedulerin
   */
  def stop(): Unit =
    this.scheduler.stop();
}

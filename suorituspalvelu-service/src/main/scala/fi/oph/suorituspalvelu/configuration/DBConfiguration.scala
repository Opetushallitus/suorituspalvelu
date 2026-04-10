package fi.oph.suorituspalvelu.configuration

import fi.oph.suorituspalvelu.business.KantaOperaatiot

import org.springframework.context.annotation.{Bean, Configuration}
import slick.jdbc.JdbcBackend
import slick.util.AsyncExecutor
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory

@Configuration
class DBConfiguration {
  private val LOG = LoggerFactory.getLogger(getClass)
  
  private val numThreads = 20
  private val queueSize = 1000

  @Bean
  def getDatabase(dataSource: HikariDataSource): JdbcBackend.JdbcDatabaseDef = {
    val maxConnections = Some(dataSource.getMaximumPoolSize)
    val slickAsyncExecutor: AsyncExecutor = AsyncExecutor("slick-executor", numThreads, queueSize)

    LOG.info(s"Configuring DB: maxConnections=$maxConnections, numThreads=$numThreads, queueSize=$queueSize")
    JdbcBackend.Database.forDataSource(dataSource, maxConnections, slickAsyncExecutor)
  }

  @Bean
  def getKantaOperaatiot(db: JdbcBackend.JdbcDatabaseDef): KantaOperaatiot =
    new KantaOperaatiot(db)
}

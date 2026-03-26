package fi.oph.suorituspalvelu.configuration

import fi.oph.suorituspalvelu.business.KantaOperaatiot

import javax.sql.DataSource
import org.springframework.context.annotation.{Bean, Configuration}
import slick.jdbc.JdbcBackend
import slick.util.AsyncExecutor

@Configuration
class DBConfiguration {

  private val numThreads = 20
  private val queueSize = 1000
  private val maxConnections = Some(25)

  private val slickAsyncExecutor: AsyncExecutor = AsyncExecutor("slick-executor", numThreads, queueSize)

  @Bean
  def getDatabase(dataSource: DataSource): JdbcBackend.JdbcDatabaseDef =
    JdbcBackend.Database.forDataSource(dataSource, maxConnections, slickAsyncExecutor)

  @Bean
  def getKantaOperaatiot(db: JdbcBackend.JdbcDatabaseDef): KantaOperaatiot =
    new KantaOperaatiot(db)
}

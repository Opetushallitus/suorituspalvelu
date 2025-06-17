package fi.oph.suorituspalvelu.configuration

import javax.sql.DataSource
import org.springframework.context.annotation.{Bean, Configuration}
import slick.jdbc.JdbcBackend

@Configuration
class DBConfiguration {

  @Bean
  def getDatabase(dataSource: DataSource): JdbcBackend.JdbcDatabaseDef =
    JdbcBackend.Database.forDataSource(dataSource, None)
}

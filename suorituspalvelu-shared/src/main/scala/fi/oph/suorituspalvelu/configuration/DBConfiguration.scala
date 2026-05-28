package fi.oph.suorituspalvelu.configuration

import fi.oph.suorituspalvelu.business.KantaOperaatiot

import javax.sql.DataSource
import org.springframework.context.annotation.{Bean, Configuration}
import slick.jdbc.JdbcBackend

@Configuration
class DBConfiguration {

  @Bean
  def getDatabase(dataSource: DataSource): JdbcBackend.JdbcDatabaseDef =
    JdbcBackend.Database.forDataSource(dataSource, None)

  @Bean
  def getKantaOperaatiot(db: JdbcBackend.JdbcDatabaseDef): KantaOperaatiot =
    new KantaOperaatiot(db)
}

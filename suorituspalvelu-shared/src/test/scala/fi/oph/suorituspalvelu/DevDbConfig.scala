package fi.oph.suorituspalvelu

import io.github.cdimascio.dotenv.Dotenv
import java.nio.file.{Files, Path, Paths}

object DevDbConfig {
  private def findProjectRoot(): Path = {
    val start = Paths.get(System.getProperty("user.dir")).toAbsolutePath
    Iterator.iterate(start)(_.getParent)
      .takeWhile(_ != null)
      .find(path => Files.exists(path.resolve(".env")))
      .getOrElse(throw new IllegalStateException(s"Could not find .env starting from $start"))
  }

  private val env = Dotenv.configure().directory(findProjectRoot().toString).load()

  val postgresImage: String = env.get("POSTGRES_IMAGE")
  val databaseName: String = env.get("POSTGRES_DB")
  val username: String = env.get("POSTGRES_USER")
  val password: String = env.get("POSTGRES_PASSWORD")
}

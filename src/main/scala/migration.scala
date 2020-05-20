import org.flywaydb.core.Flyway
import zio._
import zio.logging._

object migration {

  def migrateInternal(
      schema: String,
      jdbcUrl: String  = "jdbc:postgresql://localhost:5432/postgres",
      user: String     = "postgres",
      password: String = "postgres"
  ) =
    log.info(s"Migrating database, for schema: $schema with url: $jdbcUrl") *>
      ZIO {
        Flyway
          .configure()
          .dataSource(jdbcUrl, user, password)
          .defaultSchema(schema)
          .schemas(schema)
          .load()
          .migrate()
      }
}

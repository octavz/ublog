package org.ublog

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import org.ublog.config.Config
import zio._
import zio.blocking._
import zio.logging.Logging

object migration {

  def migrateInternal(): ZIO[Blocking with Logging with Has[Config], Throwable, MigrateResult] =
    for {
      config <- Config.storeConfig
      _      <- Logging.info(s"Migrating database, for schema: ${config.schema} with url: ${config.url}")
      result <- effectBlocking(
                 Flyway
                   .configure()
                   .dataSource(config.url, config.user, config.password)
                   .schemas(config.schema)
                   .baselineOnMigrate(true)
                   .load()
                   .migrate()
               )

    } yield result
}

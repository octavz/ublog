package org.ublog

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import org.flywaydb.core.api.output.MigrateResult
import org.ublog.web.routes
import zio._

object main extends App {

  override def run(args: List[String]) = {
    val ioMigrate: ZIO[Any, Throwable, MigrateResult] = migration.migrateInternal().provideLayer(layers.migrationLayer)
    val ioAkka: ZIO[Any, Throwable, Nothing] = Managed
      .make(Task(ActorSystem("main-akka-http")))(sys => Task.fromFuture(_ => sys.terminate()).ignore)
      .use { actorSystem =>
        implicit val system: ActorSystem = actorSystem
        ZIO.fromFuture(_ => Http().newServerAt("0.0.0.0", 8080).bind(routes(this))) *> ZIO.never
      }
    (ioMigrate *> ioAkka).orDie
  }
}

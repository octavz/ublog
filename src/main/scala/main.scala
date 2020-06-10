import zio._

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer

import scala.concurrent.ExecutionContextExecutor

object Main extends App {

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
    val ioMigrate = migration.migrateInternal("public").provideLayer(layers.logger)
    val ioAkka = Managed
      .make(Task(ActorSystem("main-akka-http")))(sys => Task.fromFuture(_ => sys.terminate()).ignore)
      .use { actorSystem =>
        implicit val system: ActorSystem                        = actorSystem
        implicit val materializer: ActorMaterializer            = ActorMaterializer()
        implicit val executionContext: ExecutionContextExecutor = system.dispatcher
        ZIO.fromFuture(_ => Http().bindAndHandle(routes(this), "0.0.0.0", 8080)) *> ZIO.never
      }
    (ioMigrate *> ioAkka).fold(_ => 1, _ => 0)
  }
}

import zio._
import zio.logging.Logging.Logging
import zio.logging._

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

import scala.concurrent.ExecutionContextExecutor
import spray.json.DefaultJsonProtocol._
import zio.logging.slf4j.Slf4jLogger
import spray.json.RootJsonFormat
import akka.http.scaladsl.model.StatusCodes
import zio.blocking.Blocking
import zio.clock.Clock
import io.github.gaelrenoux.tranzactio.doobie.Database
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.HikariConfig
import models.Post

object models {

  case class Post(id: String, title: String, content: String, author: String)
}

object serde {
  import models._

  implicit val postFmt: RootJsonFormat[Post] = rootFormat(
    lazyFormat(jsonFormat(Post, "id", "title", "content", "author"))
  )
}

object logic {
  import models._
  type Logic = Has[Module.Service]

  object Module {
    trait Service {
      def createPost(post: Post): Task[Unit]
      def getPosts(): Task[List[Post]]
    }
  }

  def createPost(post: Post) = ZIO.accessM[Logic](_.get.createPost(post))
  def getPosts()             = ZIO.accessM[Logic](_.get.getPosts())

  def test: ZLayer[db.Db with Logging.Logging, Nothing, Logic] =
    ZLayer.fromServices[db.Module.Service, Logging.Service, logic.Module.Service](
      (d: db.Module.Service, l: Logging.Service) =>
        new Module.Service {
          def createPost(post: Post): Task[Unit] = d.insert(post).as(())
          def getPosts(): Task[List[Post]] = {
            l.logger.log(LogLevel.Debug)("xyz") *> d.selectAll()
          }
        }
    )
}

object db {
  import zio.ZIO
  import models._
  import doobie.implicits._
  import io.github.gaelrenoux.tranzactio._
  import io.github.gaelrenoux.tranzactio.doobie._
  import io.github.gaelrenoux.tranzactio.doobie.Database

  type Db = Has[Module.Service]

  object Module {
    trait Service {
      def selectAll(): Task[List[Post]]
      def insert(post: Post): ZIO[Any, Throwable, Int]
    }
  }

  def selectAll()        = ZIO.accessM[Db](_.get.selectAll())
  def insert(post: Post) = ZIO.accessM[Db](_.get.insert(post))

  def insertSql(post: Post) = tzio {
    sql"INSERT INTO post(id, title, content, author) VALUES (${post.id}, ${post.title}, ${post.content}, ${post.author})".update.run
  }

  def selectAllSql() = tzio {
    sql"select id, title, content, author from post".query[Post].to[List]
  }

  def test: ZLayer[Logging.Logging with Database, Nothing, Db] =
    ZLayer.fromFunction(
      (l: Logging.Logging with Database) =>
        new Module.Service {
          override def selectAll =
            l.get[Logging.Service].logger.log(LogLevel.Debug)("Selecting posts") *>
              l.get[Database.Service].transactionOrWiden(selectAllSql())

          override def insert(post: Post) =
            l.get[Database.Service].transactionOrWiden(insertSql(post))
        }
    )
}

object Transaction {

  val config = new HikariConfig()
  config.setJdbcUrl("jdbc:postgresql://localhost:5432/postgres")
  config.setUsername("postgres")
  config.setPassword("postgres")
  val datasource: javax.sql.DataSource = new HikariDataSource(config)

  val dbLayer: ZLayer[Blocking with Clock, Nothing, Database] = Database.fromDatasource(datasource)

}

object routes {
  import models._
  import serde._

  type Env = logic.Logic with Logging
  val logger: ZLayer[Any, Nothing, Logging.Logging] = Slf4jLogger.make((_, message) => message)
  val dbL: ZLayer[Any, Nothing, db.Db]              = (((Blocking.live ++ Clock.live) >>> Transaction.dbLayer) ++ logger) >>> db.test
  val env: ZLayer[Any, Nothing, logic.Logic]        = (logger ++ dbL) >>> logic.test

  def apply(runtime: zio.Runtime[Any]) =
    get {
      path("posts") {
        complete {
          val io = log.debug("stuff").flatMap(_ => logic.getPosts()).provideLayer(env ++ logger)
          runtime.unsafeRunToFuture(io)
        }
      }
    } ~
      post {
        path("post") {
          entity(as[Post]) { post =>
            complete {
              val io = logic.createPost(post).provideLayer(env)
              val r  = io.fold(_ => StatusCodes.InternalServerError, _ => StatusCodes.Created)
              runtime.unsafeRunToFuture(r)
            }
          }
        }
      }
}

object Main extends App {

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
    val ioMigrate = migration.migrateInternal("public").provideLayer(routes.logger)
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

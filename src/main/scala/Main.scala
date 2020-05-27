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
import dev.profunktor.redis4cats.RedisCommands

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
      def getById(id: String): Task[Option[Post]]
    }
  }

  def selectAll()         = ZIO.accessM[Db](_.get.selectAll())
  def insert(post: Post)  = ZIO.accessM[Db](_.get.insert(post))
  def getById(id: String) = ZIO.accessM[Db](_.get.getById(id))

  def insertSql(post: Post) = tzio {
    sql"INSERT INTO post(id, title, content, author) VALUES (${post.id}, ${post.title}, ${post.content}, ${post.author})".update.run
  }

  def selectAllSql() = tzio {
    sql"select id, title, content, author from post".query[Post].to[List]
  }

  def getByIdSql(id: String) = tzio {
    sql"select id, title, content, author from post where id = $id".query[Post].option
  }

  def test: ZLayer[Logging.Logging with Database with Cache.Cache, Nothing, Db] =
    ZLayer.fromFunction(
      (l: Logging.Logging with Database with Cache.Cache) =>
        new Module.Service {
          override def selectAll =
            l.get[Logging.Service].logger.log(LogLevel.Debug)("Selecting posts") *>
              l.get[Database.Service].transactionOrWiden(selectAllSql())

          override def insert(post: Post) =
            l.get[Database.Service].transactionOrWiden(insertSql(post))

          override def getById(id: String) =
            // if inCache cache.get(id) else
            for {
              maybePostInCache <- l.get[Cache.Service].read(id)
              post <- if (maybePostInCache.isDefined) ZIO.succeed(maybePostInCache)
                     else l.get[Database.Service].transactionOrWiden(getByIdSql(id))
            } yield post

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

object Cache {
  import models._
  import serde._
  import spray.json._

  type Cache = Has[Service]

  trait Service {
    def write(post: Post): Task[Unit]
    def read(id: String): Task[Option[models.Post]]
  }

  def write(post: models.Post) = ZIO.accessM[Cache](_.get.write(post))
  def read(id: String)         = ZIO.accessM[Cache](_.get.read(id))

  val live: ZLayer[RedisOps.RedisOps, Nothing, Cache] =
    ZLayer.fromFunction { env: RedisOps.RedisOps =>
      new Service {
        def write(post: Post) = env.get.set(post.id, post.toJson.compactPrint)
        def read(id: String) =
          for {
            maybePostStr <- env.get.get(id)
            post         <- ZIO(maybePostStr.map(_.parseJson.convertTo[models.Post]))
            //TODO: Add result to cache
          } yield post
      }
    }
}

object Config {
  type Config = Has[Service]

  trait Service {
    val redisUri: String
  }

  def redisUri: RIO[Config, String] = ZIO.access(_.get.redisUri)

  val live: ZLayer[Any, Nothing, Config] = ZLayer.succeed(new Service {

    override val redisUri: String = "redis://localhost"

  })
}

object RedisOps {
  import models._
  import dev.profunktor.redis4cats.Redis
  import zio.interop.catz._
  import dev.profunktor.redis4cats.effect.Log
  import layers._

  type RedisCmds = Has[RedisCommands[Task, String, String]]
  type RedisOps  = Has[Service]

  val redisCmdsLive: ZLayer[Config.Config, Nothing, RedisOps.RedisCmds] =
    ZLayer.fromManaged(Config.redisUri.toManaged_.flatMap(u => Redis[Task].utf8(u).toManagedZIO).orDie)

  trait Service {
    def get(id: String): Task[Option[String]]
    def set(id: String, post: String): Task[Unit]
  }

  implicit val zioLog: Log[Task] = new Log[Task] {

    override def info(msg: => String): Task[Unit] = log.info(msg).provideLayer(layers.logger)

    override def error(msg: => String): Task[Unit] = log.error(msg).provideLayer(layers.logger)

  }

  def set(id: String, post: String): RIO[RedisOps, Unit] =
    ZIO.accessM[RedisOps](_.get.set(id, post))

  def get(id: String): RIO[RedisOps, Option[String]] =
    ZIO.accessM[RedisOps](_.get.get(id))

  val live: ZLayer[RedisCmds, Nothing, RedisOps] =
    ZLayer.fromFunction { (env: RedisCmds) =>
      new Service {

        override def get(id: String): Task[Option[String]] =
          env.get.get(id)

        override def set(id: String, post: String): Task[Unit] =
          env.get.set(post, id)

      }
    }

}

object layers {

  val logger: ZLayer[Any, Nothing, Logging.Logging] = Slf4jLogger.make((_, message) => message)
  val cacheL
      : ZLayer[Any, Nothing, Cache.Cache] = Config.live >>> RedisOps.redisCmdsLive >>> RedisOps.live >>> Cache.live
  val dbL
      : ZLayer[Any, Nothing, db.Db]          = (((Blocking.live ++ Clock.live) >>> Transaction.dbLayer) ++ layers.logger ++ cacheL) >>> db.test
  val env: ZLayer[Any, Nothing, logic.Logic] = (logger ++ dbL) >>> logic.test

}

object routes {
  import models._
  import serde._
  import layers._

  type Env = logic.Logic with Logging

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

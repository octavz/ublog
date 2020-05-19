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

object models {

  val posts: List[Post] = List(
    Post(
      id     = "1",
      title  = "Hello from μBlog",
      text   = "This is a test message, it should be no longer than N characters",
      author = "John Doe",
      comments = List(
        Post(
          id     = "11",
          title  = "Comment 1",
          text   = "This is a comment message for post 1",
          author = "Another John Doe",
          comments = List(
            Post(
              id       = "111",
              title    = "Comment 2",
              text     = "This is a comment message for comment 1",
              author   = "The pope",
              comments = Nil
            )
          )
        )
      )
    )
  )

  case class Post(id: String, title: String, text: String, author: String, comments: List[Post])
}

object serde {
  import models._

  implicit val postFmt: RootJsonFormat[Post] = rootFormat(
    lazyFormat(jsonFormat(Post, "id", "title", "text", "author", "comments"))
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
          def createPost(post: Post): Task[Unit] = Task.unit
          def getPosts(): Task[List[Post]] = {
            l.logger.log(LogLevel.Debug)("xyz") *> d.selectAll()
          }
        }
    )
}

object db {
  import models._
  type Db = Has[Module.Service]

  object Module {
    trait Service {
      def selectAll(): Task[List[Post]]
      def insert(post: Post): Task[Unit]
    }
  }

  def selectAll()        = ZIO.accessM[Db](_.get.selectAll())
  def insert(post: Post) = ZIO.accessM[Db](_.get.insert(post))

  def test: ZLayer[Logging.Logging, Nothing, Db] =
    ZLayer.fromFunction(
      (l: Logging.Logging) =>
        new Module.Service {
          override def selectAll =
            l.get.logger.log(LogLevel.Debug)("Selecting posts") *>
              Task.succeed(models.posts)

          override def insert(post: Post) = Task.unit
        }
    )
}

object routes {
  import models._
  import serde._

  type Env = logic.Logic with Logging
  val logger: ZLayer[Any, Nothing, Logging.Logging] = Slf4jLogger.make((_, message) => message)
  val dbL: ZLayer[Any, Nothing, db.Db] = logger >>> db.test
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
    Managed
      .make(Task(ActorSystem("main-akka-http")))(sys => Task.fromFuture(_ => sys.terminate()).ignore)
      .use { actorSystem =>
        implicit val system: ActorSystem                        = actorSystem
        implicit val materializer: ActorMaterializer            = ActorMaterializer()
        implicit val executionContext: ExecutionContextExecutor = system.dispatcher
        ZIO.fromFuture(_ => Http().bindAndHandle(routes(this), "0.0.0.0", 8080)) *> ZIO.never
      }
      .fold(_ => 1, _ => 0)
  }
}

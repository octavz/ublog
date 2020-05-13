import zio._
import zio.logging.Logging.Logging

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

import scala.concurrent.ExecutionContextExecutor
import spray.json.DefaultJsonProtocol._
import zio.logging.slf4j.Slf4jLogger
import spray.json.RootJsonFormat
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.StatusCodes

object models {

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

  def test =
    ZLayer.succeed(new Module.Service {
      def createPost(post: Post): Task[Unit] = Task.unit
      def getPosts(): Task[List[Post]] =
        Task.succeed(
          List(
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
        )
    })

  def createPost(post: Post) = ZIO.accessM[Logic](_.get.createPost(post))
  def getPosts()             = ZIO.accessM[Logic](_.get.getPosts())
}

object routes {
  import models._
  import serde._

  type Env = logic.Logic with Logging
  val logger                                = Slf4jLogger.make((_, message) => message)
  val env: ULayer[logic.Logic with Logging] = logic.test ++ logger

  def apply(runtime: zio.Runtime[Any]) =
    get {
      path("posts") {
        complete {
          val io = logic.getPosts().provideLayer(env)
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

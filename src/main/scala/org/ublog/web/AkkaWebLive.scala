package org.ublog.web

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.ublog.logic.Logic
import org.ublog.models.Post
import org.ublog.serde._
import spray.json.DefaultJsonProtocol._
import zio._
import zio.logging.Logger

case class AkkaWebLive(runtime: zio.Runtime[Any], logic: Logic, log: Logger[String]) extends Web {

  override def startServer(): ZIO[Any, Throwable, Nothing] =
    Managed
      .make(Task(ActorSystem("main-akka-http")))(sys => Task.fromFuture(_ => sys.terminate()).ignore)
      .use { actorSystem =>
        implicit val system: ActorSystem = actorSystem
        ZIO.fromFuture(_ => Http().newServerAt("0.0.0.0", 8080).bind(routes())) *> ZIO.never
      }

  def routes(): Route =
    get {
      path("posts") {
        complete {
          val io = log.debug("stuff") *> logic.getPosts()
          runtime.unsafeRunToFuture(io)
        }
      }
    } ~
      post {
        path("post") {
          entity(as[Post]) { post =>
            complete {
              val io = logic.createPost(post)
              val r  = io.fold(_ => StatusCodes.InternalServerError, _ => StatusCodes.Created)
              runtime.unsafeRunToFuture(r)
            }
          }
        }
      } ~
      get {
        path("post" / Segment) { id =>
          complete {
            val io = logic.getPostById(id)
            runtime.unsafeRunToFuture(io)
          }
        }
      }
}

object AkkaWebLive {
  def layer(runtime: zio.Runtime[Any]): ZLayer[Has[Logic] with Has[Logger[String]], Throwable, Has[Web]] =
    ((logic: Logic, log: Logger[String]) => AkkaWebLive(runtime, logic, log)).toLayer
}

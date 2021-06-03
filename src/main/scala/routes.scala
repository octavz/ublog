import zio.logging.Logging
import zio.logging.log
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._

import layers._
import models._
import serde._

object routes {

  type Env = logic.Logic with Logging

  def apply(runtime: zio.Runtime[Any]) =
    get {
      path("posts") {
        complete {
          val io = (log.debug("stuff") *> logic.getPosts()).provideLayer(env ++ logger)
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
      } ~
      get {
        path("post" / Segment) { id =>
          complete {
            val io = logic.getPostById(id).provideLayer(env)
            runtime.unsafeRunToFuture(io)
          }
        }
      }
}

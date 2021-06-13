package org.ublog.web

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import org.ublog.layers._
import org.ublog.logic.Logic
import org.ublog.models.Post
import org.ublog.serde._
import spray.json.DefaultJsonProtocol._
import zio.logging._

object routes {

  type Env = Logic with Logging

  def apply(runtime: zio.Runtime[Any]) =
    get {
      path("posts") {
        complete {
          val io = (log.debug("stuff") *> Logic.getPosts()).provideLayer(applicationLayer ++ loggerLayer)
          runtime.unsafeRunToFuture(io)
        }
      }
    } ~
      post {
        path("post") {
          entity(as[Post]) { post =>
            complete {
              val io = Logic.createPost(post).provideLayer(applicationLayer)
              val r  = io.fold(_ => StatusCodes.InternalServerError, _ => StatusCodes.Created)
              runtime.unsafeRunToFuture(r)
            }
          }
        }
      } ~
      get {
        path("post" / Segment) { id =>
          complete {
            val io = Logic.getPostById(id).provideLayer(applicationLayer)
            runtime.unsafeRunToFuture(io)
          }
        }
      }
}

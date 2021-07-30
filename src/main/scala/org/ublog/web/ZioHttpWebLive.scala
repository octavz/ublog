package org.ublog.web

import org.ublog.logic.Logic
import org.ublog.web.models.WebPost
import zhttp.http._
import zhttp.service.Server
import zio._
import zio.logging.Logger
import zio.json._

case class ZioHttpWebLive(logic: Logic, log: Logger[String]) extends Web {
  val app: Http[Any, Nothing, Request, UResponse] = Http.collectM[Request] {
    case Method.GET -> Root / "posts" =>
      val io: ZIO[Any, Throwable, UResponse] = for {
        _     <- log.debug("zio http get method called")
        posts <- logic.getPosts()
        webPosts = posts.map(WebPost.apply)
      } yield Response.jsonString(webPosts.toJson)
      io.fold(
        t => Response.fromHttpError(HttpError.InternalServerError(t.getMessage, Some(t))),
        identity
      )
  }

  override def startServer(): ZIO[Any, Throwable, Nothing] =
    Server.start(8081, app.silent).provideLayer(ZLayer.succeed(logic))
}

object ZioHttpWebLive {
  val layer: ZLayer[Has[Logic] with Has[Logger[String]], Throwable, Has[Web]] =
    ((logic: Logic, log: Logger[String]) => ZioHttpWebLive(logic, log)).toLayer
}

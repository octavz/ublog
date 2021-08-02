package org.ublog.web

import org.ublog.logic.Logic
import org.ublog.web.models.WebPost
import zhttp.http._
import zhttp.service.Server
import zio._
import zio.json._
import zio.logging.Logger

case class ZioHttpWebLive(logic: Logic, log: Logger[String]) extends Web {

  def extractBodyFromJson[A](request: Request)(implicit codec: JsonDecoder[A]): IO[Throwable, A] = {
    val body = request.getBodyAsString
      .map(_.fromJson[A])
      .getOrElse(Left("Empty request body"))
    ZIO.fromEither(body).mapError(new Exception(_))
  }

  def handleError(io: ZIO[Any, Throwable, UResponse]): URIO[Any, UResponse] =
    io.fold(
      t => Response.fromHttpError(HttpError.InternalServerError(t.getMessage, Some(t))),
      identity
    )

  val app: Http[Any, Nothing, Request, UResponse] = Http.collectM[Request] {
    case Method.GET -> Root / "posts" =>
      handleError(for {
        _     <- log.debug("GET /posts http method called")
        posts <- logic.getPosts()
        webPosts = posts.map(WebPost.apply)
      } yield Response.jsonString(webPosts.toJson))
    case req @ Method.POST -> Root / "post" =>
      handleError(
        for {
          _       <- log.debug("POST /post http method called")
          webPost <- extractBodyFromJson[WebPost](req)
          _       <- logic.createPost(webPost.toPost)
        } yield Response.ok
      )
    case Method.GET -> Root / "post" / id =>
      handleError(
        for {
          _    <- log.debug("GET /post/<id> http method called")
          post <- logic.getPostById(id)
          webPost = post.map(WebPost.apply)
        } yield Response.jsonString(webPost.toJson)
      )
  }

  override def startServer(): ZIO[Any, Throwable, Nothing] =
    Server.start(8081, app.silent).provideLayer(ZLayer.succeed(logic))
}

object ZioHttpWebLive {
  val layer: ZLayer[Has[Logic] with Has[Logger[String]], Throwable, Has[Web]] =
    ((logic: Logic, log: Logger[String]) => ZioHttpWebLive(logic, log)).toLayer
}

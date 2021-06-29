package org.ublog

import dev.profunktor.redis4cats._
import dev.profunktor.redis4cats.effect.Log
import org.ublog.config.Config
import org.ublog.models.Post
import org.ublog.serde._
import spray.json._
import zio._
import zio.interop.catz._
import zio.logging.log

object cache {
  // service definition
  trait Cache {
    def write(post: Post): Task[Unit]
    def read(id: String): Task[Option[Post]]
  }

  // accessors = public API for the service
  object Cache {
    def write(post: Post): ZIO[Has[Cache], Throwable, Unit]        = ZIO.serviceWith[Cache](_.write(post))
    def read(id: String): ZIO[Has[Cache], Throwable, Option[Post]] = ZIO.serviceWith[Cache](_.read(id))
  }

  case class CacheLive(redisOps: RedisCommands[Task, String, String]) extends Cache {

    def write(post: Post) = redisOps.set(post.id, post.toJson.compactPrint)
    def read(id: String) =
      for {
        maybePostStr <- redisOps.get(id)
        post         <- ZIO(maybePostStr.map(_.parseJson.convertTo[Post]))
      } yield post
  }

  object CacheLive {
    val layer: ZLayer[Has[Config], Nothing, Has[Cache]] = redisCmdsLive >>> cacheLive

    private def cacheLive: ZLayer[Has[RedisCommands[Task, String, String]], Nothing, Has[Cache]] =
      (CacheLive(_)).toLayer

    private def redisCmdsLive: ZLayer[Has[Config], Nothing, Has[RedisCommands[Task, String, String]]] =
      ZLayer.fromManaged(Config.redisUri.toManaged_.flatMap(u => Redis[Task].utf8(u).toManagedZIO).orDie)

    // required by Redis[Task] constructor
    implicit val zioLog: Log[Task] = new Log[Task] {
      override def info(msg: => String): Task[Unit]  = log.info(msg).provideLayer(layers.loggerLayer)
      override def error(msg: => String): Task[Unit] = log.error(msg).provideLayer(layers.loggerLayer)
      override def debug(msg: => String): Task[Unit] = log.debug(msg).provideLayer(layers.loggerLayer)
    }
  }
}

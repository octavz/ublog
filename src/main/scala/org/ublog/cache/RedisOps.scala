package org.ublog.cache

import org.ublog.config.Config
import org.ublog.layers
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.effect.Log
import zio._
import zio.interop.catz._
import zio.logging.log

object RedisOps {

  // service definition
  trait Service {
    def get(id: String): Task[Option[String]]
    def set(id: String, post: String): Task[Unit]
  }

  // accessors
  def set(id: String, post: String): RIO[RedisOps, Unit] = ZIO.accessM[RedisOps](_.get.set(id, post))
  def get(id: String): RIO[RedisOps, Option[String]]     = ZIO.accessM[RedisOps](_.get.get(id))

  // implementations
  val live: ZLayer[RedisCmds, Nothing, RedisOps] =
    ZLayer.fromFunction { (env: RedisCmds) =>
      new Service {
        override def get(id: String): Task[Option[String]]     = env.get.get(id)
        override def set(id: String, post: String): Task[Unit] = env.get.set(post, id)
      }
    }

  val redisCmdsLive: ZLayer[Config, Nothing, RedisCmds] =
    ZLayer.fromManaged(Config.redisUri.toManaged_.flatMap(u => Redis[Task].utf8(u).toManagedZIO).orDie)

  implicit val zioLog: Log[Task] = new Log[Task] {
    override def info(msg: => String): Task[Unit]  = log.info(msg).provideLayer(layers.loggerLayer)
    override def error(msg: => String): Task[Unit] = log.error(msg).provideLayer(layers.loggerLayer)
    override def debug(msg: => String): Task[Unit] = log.debug(msg).provideLayer(layers.loggerLayer)
  }
}

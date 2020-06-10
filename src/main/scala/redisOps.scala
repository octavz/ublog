import zio._
import dev.profunktor.redis4cats.effect.Log
import dev.profunktor.redis4cats.RedisCommands
import zio.interop.catz._
import zio.logging._
import dev.profunktor.redis4cats.Redis

object redisOps {

  type RedisCmds = Has[RedisCommands[Task, String, String]]
  type RedisOps  = Has[Service]

  val redisCmdsLive: ZLayer[config.Config, Nothing, redisOps.RedisCmds] =
    ZLayer.fromManaged(config.redisUri.toManaged_.flatMap(u => Redis[Task].utf8(u).toManagedZIO).orDie)

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

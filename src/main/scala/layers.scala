import zio._
import zio.logging.slf4j.Slf4jLogger
import zio.logging.Logging

object layers {

  def logger: ZLayer[Any, Nothing, Logging] = Slf4jLogger.make((_, message) => message)
  def cacheL: ZLayer[Any, Nothing, cache.Cache] =
    config.live >>> redisOps.redisCmdsLive >>> redisOps.live >>> cache.live
  lazy val dbL: ZLayer[Any, Nothing, db.Db] =
    (transaction.dbLayer ++ layers.logger ++ cacheL) >>> db.live
  def env: ZLayer[Any, Nothing, logic.Logic] =
    (logger ++ dbL) >>> logic.live

}

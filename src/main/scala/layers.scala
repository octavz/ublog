import zio._
import zio.logging.slf4j.Slf4jLogger
import zio.clock.Clock
import zio.blocking.Blocking
import zio.logging.Logging

object layers {

  val logger: ZLayer[Any, Nothing, Logging.Logging] = Slf4jLogger.make((_, message) => message)
  val cacheL: ZLayer[Any, Nothing, cache.Cache] =
    config.live >>> redisOps.redisCmdsLive >>> redisOps.live >>> cache.live
  val dbL: ZLayer[Any, Nothing, db.Db] =
    (((Blocking.live ++ Clock.live) >>> transaction.dbLayer) ++ layers.logger ++ cacheL) >>> db.test
  val env: ZLayer[Any, Nothing, logic.Logic] =
    (logger ++ dbL) >>> logic.live

}

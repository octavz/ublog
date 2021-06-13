package org.ublog

import org.ublog.cache._
import org.ublog.config.Config
import org.ublog.data.Data
import org.ublog.logic.Logic
import org.ublog.persistence.Persistence
import org.ublog.persistence.models.PersistenceException
import zio.ZLayer
import zio.blocking.Blocking
import zio.clock.Clock
import zio.logging.Logging
import zio.logging.slf4j.Slf4jLogger

object layers {

  def loggerLayer: ZLayer[Any, Nothing, Logging] = Slf4jLogger.make((_, message) => message)

  def migrationLayer: ZLayer[Any, Nothing, Logging with Blocking with Config] =
    loggerLayer ++ Blocking.live ++ Config.live

  def cacheLayer: ZLayer[Any, Nothing, cache.Cache] =
    Config.live >>> RedisOps.redisCmdsLive >>> RedisOps.live >>> Cache.live

  def persistenceLayer: ZLayer[Any, PersistenceException, Persistence] =
    (loggerLayer ++ Blocking.live ++ Clock.live ++ Config.live) >>> Persistence.live

  def dataLayer: ZLayer[Any, PersistenceException, Data] =
    (loggerLayer ++ persistenceLayer ++ cacheLayer) >>> Data.live

  def applicationLayer: ZLayer[Any, Throwable, logic.Logic] =
    (loggerLayer ++ dataLayer) >>> Logic.live

}

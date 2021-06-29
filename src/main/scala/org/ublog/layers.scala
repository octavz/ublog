package org.ublog

import org.ublog.cache._
import org.ublog.config._
import org.ublog.data._
import org.ublog.logic._
import org.ublog.metrics.MetricsLive
import org.ublog.persistence._
import org.ublog.persistence.models.PersistenceException
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.logging.Logging
import zio.logging.slf4j.Slf4jLogger

object layers {

  def loggerLayer: ZLayer[Any, Nothing, Logging] = Slf4jLogger.make((_, message) => message)

  def migrationLayer: ZLayer[Any, Nothing, Logging with Blocking with Has[Config]] =
    loggerLayer ++ Blocking.live ++ ConfigLive.layer

  def cacheLayer: ZLayer[Any, Nothing, Has[Cache]] = ConfigLive.layer >>> CacheLive.layer

  def persistenceLayer: ZLayer[Any, PersistenceException, Has[Persistence]] =
    (loggerLayer ++ Blocking.live ++ Clock.live ++ ConfigLive.layer) >>> PersistenceLive.layer

  def dataLayer: ZLayer[Any, PersistenceException, Has[Data]] =
    (loggerLayer ++ persistenceLayer ++ cacheLayer) >>> DataLive.layer

  def applicationLayer: ZLayer[Any, Throwable, Has[Logic]] =
    (loggerLayer ++ dataLayer ++ MetricsLive.layer) >>> LogicLive.layer

}

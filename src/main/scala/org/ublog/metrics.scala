package org.ublog

import kamon.Kamon
import zio._

object metrics {

  trait Metrics {
    def incrementCreatedPosts(): UIO[Unit]
  }

  // accessors kinda become obsolete if the service is used only as a dependency and never as a main effect
  object Metrics {
    def incrementCreatedPosts(): ZIO[Has[Metrics], Nothing, Unit] = ZIO.serviceWith[Metrics](_.incrementCreatedPosts())
  }

  case class MetricsLive() extends Metrics {

    override def incrementCreatedPosts(): UIO[Unit] = ZIO.succeed(Kamon.counter("posts").withoutTags().increment()).unit
  }

  object MetricsLive {
    val layer: ZLayer[Any, Nothing, Has[Metrics]] = (() => MetricsLive()).toLayer
  }

}

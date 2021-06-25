package org.ublog

import zio._

object config {
  case class StoreConfig(url: String, schema: String, user: String, password: String)

  // service definition
  trait Config {
    val redisUri: String
    val storeConfig: StoreConfig
  }
  // accessors = public API for the service
  object Config {
    def redisUri: RIO[Has[Config], String]         = ZIO.access[Has[Config]](_.get.redisUri)
    def storeConfig: RIO[Has[Config], StoreConfig] = ZIO.access[Has[Config]](_.get.storeConfig)
  }

  case class ConfigLive() extends Config {
    override val redisUri: String = "redis://localhost"
    override val storeConfig: StoreConfig = StoreConfig(
      url      = "jdbc:postgresql://localhost:5432/postgres",
      schema   = "public",
      user     = "postgres",
      password = "postgres"
    )
  }

  object ConfigLive {
    val layer: ZLayer[Any, Nothing, Has[Config]] = (() => ConfigLive()).toLayer
  }
}

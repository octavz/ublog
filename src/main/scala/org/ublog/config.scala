package org.ublog

import zio._

object config {
  type Config = Has[Config.Service]

  case class StoreConfig(url: String, schema: String, user: String, password: String)

  object Config {
    // service definition
    trait Service {
      val redisUri: String
      val storeConfig: StoreConfig
    }
    // accessors
    def redisUri: RIO[Config, String]         = ZIO.access(_.get.redisUri)
    def storeConfig: RIO[Config, StoreConfig] = ZIO.access(_.get.storeConfig)

    // implementation
    val live: ZLayer[Any, Nothing, Config] = ZLayer.succeed(new Service {
      override val redisUri: String = "redis://localhost"
      override val storeConfig: StoreConfig = StoreConfig(
        url      = "jdbc:postgresql://localhost:5432/postgres",
        schema   = "public",
        user     = "postgres",
        password = "postgres"
      )
    })
  }

}

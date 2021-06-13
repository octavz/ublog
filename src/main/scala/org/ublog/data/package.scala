package org.ublog

import com.zaxxer.hikari._
import io.github.gaelrenoux.tranzactio.doobie
import io.github.gaelrenoux.tranzactio.doobie._
import org.ublog.cache.Cache
import org.ublog.config.Config
import org.ublog.models.Post
import org.ublog.persistence.Persistence
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.logging._

import javax.sql.DataSource

package object data {
  type Data = Has[Data.Service]

  object Data {
    // service definition
    trait Service {
      def selectAll(): Task[List[Post]]
      def insert(post: Post): Task[Unit]
      def getById(id: String): Task[Option[Post]]
    }

    // accessors = public API for the service
    def selectAll(): ZIO[Data, Throwable, List[Post]]           = ZIO.accessM[Data](_.get.selectAll())
    def insert(post: Post): ZIO[Data, Throwable, Unit]          = ZIO.accessM[Data](_.get.insert(post))
    def getById(id: String): ZIO[Data, Throwable, Option[Post]] = ZIO.accessM[Data](_.get.getById(id))

    // implementation
    def live: ZLayer[Logging with Persistence with Cache, Nothing, Data] =
      ZLayer.fromFunction(
        (l: Logging with Persistence with Cache) =>
          new LiveDataService {
            override val cacheService: Cache.Service             = l.get[Cache.Service]
            override val persistenceService: Persistence.Service = l.get[Persistence.Service]
            override val logger: Logger[String]                  = l.get[Logger[String]]
          }
      )

    def dataSourceLive: ZLayer[Config with Blocking, Throwable, Has[DataSource]] =
      (for {
        store <- Config.storeConfig
        datasource <- blocking.effectBlocking {
                       val config = new HikariConfig()
                       config.setJdbcUrl(store.url)
                       config.setUsername(store.user)
                       config.setPassword(store.password)
                       new HikariDataSource(config)
                     }
      } yield datasource).toLayer

    val databaseLayer: ZLayer[Has[DataSource] with Blocking with Clock, Nothing, doobie.Database.Database] =
      Database.fromDatasource
  }
}

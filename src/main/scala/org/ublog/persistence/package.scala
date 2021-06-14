package org.ublog

import com.zaxxer.hikari._
import io.github.gaelrenoux.tranzactio.doobie
import org.ublog.config.Config
import org.ublog.persistence.models._
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.logging._

import javax.sql.DataSource

package object persistence {
  type Persistence = Has[Persistence.Service]

  object Persistence {
    // service definition
    trait Service {
      def getAllPosts(): IO[PersistenceException, List[DbPost]]
      def getPostById(id: String): IO[PersistenceException, Option[DbPost]]
      def insertPost(post: DbPost): IO[PersistenceException, Unit]
    }

    // accessors
    def getAllPosts(): ZIO[Persistence, PersistenceException, List[DbPost]] =
      ZIO.accessM[Persistence](_.get.getAllPosts())
    def getPostById(id: String): ZIO[Persistence, PersistenceException, Option[DbPost]] =
      ZIO.accessM[Persistence](_.get.getPostById(id))
    def insertPost(post: DbPost): ZIO[Persistence, PersistenceException, Unit] =
      ZIO.accessM[Persistence](_.get.insertPost(post))

    // implementation layer - independent of implementation details
    val live: ZLayer[Logging with Blocking with Clock with Config, PersistenceException, Persistence] =
      (Logging.any ++ databaseLive) >>> persistenceLive

    // our implementation dependent of tranzactio database layer
    private def persistenceLive: ZLayer[Logging with doobie.Database.Database, Nothing, Persistence] =
      ZLayer.fromServices[Logger[String], doobie.Database.Service, Persistence.Service] {
        (log: Logger[String], database: doobie.Database.Service) =>
          LivePersistenceService(log, database)
      }

    // Tranzactio database layer
    private def databaseLive: ZLayer[Blocking with Clock with Config, PersistenceException, doobie.Database.Database] =
      (Blocking.any ++ Clock.any ++ dataSourceLive) >>> doobie.Database.fromDatasource

    // DataSource layer required to create a transactio database
    private def dataSourceLive: ZLayer[Config with Blocking, PersistenceException, Has[DataSource]] =
      (for {
        store <- Config.storeConfig
        datasource <- blocking.effectBlocking {
                       val config = new HikariConfig()
                       config.setJdbcUrl(store.url)
                       config.setUsername(store.user)
                       config.setPassword(store.password)
                       new HikariDataSource(config)
                     }
      } yield datasource)
        .mapError(PersistenceException(_))
        .toLayer
  }

}

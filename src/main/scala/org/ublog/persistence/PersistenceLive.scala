package org.ublog.persistence

import com.zaxxer.hikari._
import doobie.implicits.toSqlInterpolator
import io.github.gaelrenoux.tranzactio.doobie
import io.github.gaelrenoux.tranzactio.doobie._
import org.ublog.config.Config
import org.ublog.persistence.models._
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.logging._

import javax.sql.DataSource

case class PersistenceLive(logger: Logger[String], database: Database.Service) extends Persistence {

  override def getAllPosts(): IO[PersistenceException, List[DbPost]] =
    logger.debug("Selecting posts") *>
      database
        .transactionOrWiden(
          tzio {
            sql"select id, title, content, author from post".query[DbPost].to[List]
          }
        )
        .mapError(PersistenceException(_))

  override def getPostById(id: String): IO[PersistenceException, Option[DbPost]] =
    database
      .transactionOrWiden(
        tzio {
          sql"select id, title, content, author from post where id = $id".query[DbPost].option
        }
      )
      .mapError(PersistenceException(_))

  override def insertPost(post: DbPost): IO[PersistenceException, Unit] =
    database
      .transactionOrWiden(
        tzio {
          sql"INSERT INTO post(id, title, content, author) VALUES (${post.id}, ${post.title}, ${post.content}, ${post.author})".update.run
        }
      )
      .mapError(PersistenceException(_))
      .unit
}

object PersistenceLive {
  val layer: ZLayer[Logging with Blocking with Clock with Has[Config], PersistenceException, Has[Persistence]] =
    (Logging.any ++ databaseLive) >>> persistenceLive

  // our implementation dependent of tranzactio database layer
  private def persistenceLive: URLayer[Has[Logger[String]] with Has[doobie.Database.Service], Has[Persistence]] =
    (
        (
            log: Logger[String],
            database: doobie.Database.Service
        ) => PersistenceLive(log, database)
    ).toLayer

  // Tranzactio database layer
  private def databaseLive
      : ZLayer[Blocking with Clock with Has[Config], PersistenceException, doobie.Database.Database] =
    (Blocking.any ++ Clock.any ++ dataSourceLive) >>> doobie.Database.fromDatasource

  // DataSource layer required to create a transactio database
  private def dataSourceLive: ZLayer[Has[Config] with Blocking, PersistenceException, Has[DataSource]] =
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

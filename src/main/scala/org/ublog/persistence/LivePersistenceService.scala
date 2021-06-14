package org.ublog.persistence

import doobie.implicits.toSqlInterpolator
import io.github.gaelrenoux.tranzactio.doobie._
import org.ublog.persistence.models._
import zio._
import zio.logging.Logger

case class LivePersistenceService(logger: Logger[String], database: Database.Service) extends Persistence.Service {

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

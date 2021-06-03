import zio._
import zio.logging._
import doobie.implicits._
import io.github.gaelrenoux.tranzactio.doobie._
import io.github.gaelrenoux.tranzactio.doobie.Database

import cache._
import models.Post

object db {

  type Db = Has[Db.Service]

  object Db {
    trait Service {
      def selectAll(): Task[List[Post]]
      def insert(post: Post): ZIO[Any, Throwable, Int]
      def getById(id: String): Task[Option[Post]]
    }
  }

  def selectAll()         = ZIO.accessM[Db](_.get.selectAll())
  def insert(post: Post)  = ZIO.accessM[Db](_.get.insert(post))
  def getById(id: String) = ZIO.accessM[Db](_.get.getById(id))

  def insertSql(post: Post) = tzio {
    sql"INSERT INTO post(id, title, content, author) VALUES (${post.id}, ${post.title}, ${post.content}, ${post.author})".update.run
  }

  def selectAllSql() = tzio {
    sql"select id, title, content, author from post".query[Post].to[List]
  }

  def getByIdSql(id: String) = tzio {
    sql"select id, title, content, author from post where id = $id".query[Post].option
  }

  def live: ZLayer[Logging with Database with cache.Cache, Nothing, Db] =
    ZLayer.fromFunction(
      (l: Logging with Database with cache.Cache) =>
        new Db.Service {
          val myCache = l.get[Cache.Service]
          val db      = l.get[Database.Service]
          val logger  = l.get[Logger[String]]

          override def selectAll() =
            logger.log(LogLevel.Debug)("Selecting posts") *>
              db.transactionOrWiden(selectAllSql())

          override def insert(post: Post) =
            db.transactionOrWiden(insertSql(post))

          override def getById(id: String) =
            for {
              maybePostInCache <- myCache.read(id)
              post <- if (maybePostInCache.isDefined) ZIO.succeed(maybePostInCache)
                     else
                       for {
                         maybePost <- db.transactionOrWiden(getByIdSql(id))
                         _ <- maybePost match {
                               case Some(v) => myCache.write(v)
                               case _ => ZIO.unit
                             }
                       } yield maybePost
            } yield post
        }
    )
}

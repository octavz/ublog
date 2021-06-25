package org.ublog.data

import org.ublog.cache.Cache
import org.ublog.models
import org.ublog.models._
import org.ublog.persistence._
import org.ublog.persistence.models.PersistenceException
import zio._
import zio.logging._

case class DataLive(cacheService: Cache, persistenceService: Persistence, logger: Logger[String]) extends Data {

  override def selectAll(): IO[PersistenceException, List[Post]] =
    logger.debug("Selecting posts") *> persistenceService.getAllPosts().map(_.map(models.fromDb))

  override def insert(post: Post): IO[PersistenceException, Unit] = persistenceService.insertPost(post.toDb)

  override def getById(id: String): IO[PersistenceException, Option[Post]] =
    (for {
      maybePostInCache <- cacheService.read(id)
      post <- if (maybePostInCache.isDefined)
               IO.succeed(maybePostInCache)
             else
               for {
                 maybeDbPost <- persistenceService.getPostById(id)
                 maybePost = maybeDbPost.map(models.fromDb)
                 _ <- maybePost match {
                       case Some(v) => cacheService.write(v)
                       case _ => ZIO.unit
                     }
               } yield maybePost
    } yield post)
      .mapError(PersistenceException(_))

}

object DataLive {
  val layer: ZLayer[Logging with Has[Persistence] with Has[Cache], Nothing, Has[Data]] =
    (
        (
            l: Logger[String],
            p: Persistence,
            c: Cache
        ) => new DataLive(cacheService = c, persistenceService = p, logger = l)
    ).toLayer
}

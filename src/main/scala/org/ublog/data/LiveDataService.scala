package org.ublog.data

import org.ublog.cache.Cache
import org.ublog.models
import org.ublog.models._
import org.ublog.persistence.Persistence
import zio._
import zio.logging._

trait LiveDataService extends Data.Service {
  val cacheService: Cache.Service
  val persistenceService: Persistence.Service
  val logger: Logger[String]

  override def selectAll() =
    logger.debug("Selecting posts") *> persistenceService.getAllPosts().map(_.map(models.fromDb))

  override def insert(post: Post) = persistenceService.insertPost(post.toDb())

  override def getById(id: String) =
    for {
      maybePostInCache <- cacheService.read(id)
      post <- if (maybePostInCache.isDefined)
               ZIO.succeed(maybePostInCache)
             else
               for {
                 maybeDbPost <- persistenceService.getPostById(id)
                 maybePost = maybeDbPost.map(models.fromDb)
                 _ <- maybePost match {
                       case Some(v) => cacheService.write(v)
                       case _ => ZIO.unit
                     }
               } yield maybePost
    } yield post

}

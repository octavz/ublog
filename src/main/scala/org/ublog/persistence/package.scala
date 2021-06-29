package org.ublog

import org.ublog.persistence.models._
import zio._

package object persistence {
  // service definition
  trait Persistence {
    def getAllPosts(): IO[PersistenceException, List[DbPost]]
    def getPostById(id: String): IO[PersistenceException, Option[DbPost]]
    def insertPost(post: DbPost): IO[PersistenceException, Unit]
  }

  // accessors = public API for the service
  object Persistence {
    def getAllPosts(): ZIO[Has[Persistence], PersistenceException, List[DbPost]] =
      ZIO.serviceWith[Persistence](_.getAllPosts())
    def getPostById(id: String): ZIO[Has[Persistence], PersistenceException, Option[DbPost]] =
      ZIO.serviceWith[Persistence](_.getPostById(id))
    def insertPost(post: DbPost): ZIO[Has[Persistence], PersistenceException, Unit] =
      ZIO.serviceWith[Persistence](_.insertPost(post))
  }
}

package org.ublog

import org.ublog.models.Post
import zio._

package object data {
  // service definition
  trait Data {
    def selectAll(): Task[List[Post]]
    def insert(post: Post): Task[Unit]
    def getById(id: String): Task[Option[Post]]
  }

  // accessors = public API for the service
  object Data {
    def selectAll(): ZIO[Has[Data], Throwable, List[Post]]           = ZIO.serviceWith[Data](_.selectAll())
    def insert(post: Post): ZIO[Has[Data], Throwable, Unit]          = ZIO.serviceWith[Data](_.insert(post))
    def getById(id: String): ZIO[Has[Data], Throwable, Option[Post]] = ZIO.serviceWith[Data](_.getById(id))
  }
}

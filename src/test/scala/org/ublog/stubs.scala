package org.ublog

import org.ublog.data.Data
import org.ublog.models.Post
import zio._

object stubs {
  // Service uses a Ref to mark the call of the insert function
  def stubDataService(value: Ref[Int]): Data = new Data {
    override def selectAll(): Task[List[Post]] = ???

    override def insert(post: Post): Task[Unit] = value.update(_ + 1)

    override def getById(id: String): Task[Option[Post]] = ???
  }

}

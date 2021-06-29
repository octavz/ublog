package org.ublog

import org.ublog.data._
import org.ublog.metrics.Metrics
import org.ublog.models.Post
import zio._
import zio.logging._

object logic {

  // service definition
  trait Logic {
    def createPost(post: Post): Task[Unit]
    def getPosts(): Task[List[Post]]
    def getPostById(id: String): Task[Option[Post]]
  }

  object Logic {
    // accessors
    def createPost(post: Post): ZIO[Has[Logic], Throwable, Unit]          = ZIO.serviceWith[Logic](_.createPost(post))
    def getPosts(): ZIO[Has[Logic], Throwable, List[Post]]                = ZIO.serviceWith[Logic](_.getPosts())
    def getPostById(id: String): ZIO[Has[Logic], Throwable, Option[Post]] = ZIO.serviceWith[Logic](_.getPostById(id))
  }

  case class LogicLive(d: Data, l: Logger[String], m: Metrics) extends Logic {
    def createPost(post: Post): Task[Unit] =
      l.debug("creating post") *> d.insert(post).unit *> m.incrementCreatedPosts()
    def getPosts(): Task[List[Post]]                = l.debug("Selecting posts") *> d.selectAll()
    def getPostById(id: String): Task[Option[Post]] = l.debug(s"Selecting post for id $id") *> d.getById(id)
  }

  object LogicLive {
    val layer: ZLayer[Has[Data] with Has[Logger[String]] with Has[Metrics], Nothing, Has[Logic]] =
      (LogicLive(_, _, _)).toLayer
  }
}

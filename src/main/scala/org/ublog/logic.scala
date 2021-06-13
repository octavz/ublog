package org.ublog

import org.ublog.data._
import org.ublog.models.Post
import zio._
import zio.logging._

object logic {
  type Logic = Has[Logic.Service]

  object Logic {
    // service definition
    trait Service {
      def createPost(post: Post): Task[Unit]
      def getPosts(): Task[List[Post]]
      def getPostById(id: String): Task[Option[Post]]
    }

    // accessors
    def createPost(post: Post): ZIO[Logic, Throwable, Unit]          = ZIO.accessM[Logic](_.get.createPost(post))
    def getPosts(): ZIO[Logic, Throwable, List[Post]]                = ZIO.accessM[Logic](_.get.getPosts())
    def getPostById(id: String): ZIO[Logic, Throwable, Option[Post]] = ZIO.accessM[Logic](_.get.getPostById(id))

    // implementation
    def live: ZLayer[Data with Logging, Nothing, Logic] =
      ZLayer.fromServices[Data.Service, Logger[String], logic.Logic.Service](
        (d: Data.Service, l: Logger[String]) =>
          new Logic.Service {
            def createPost(post: Post): Task[Unit]          = l.debug("creating post") *> d.insert(post).unit
            def getPosts(): Task[List[Post]]                = l.debug("Selecting posts") *> d.selectAll()
            def getPostById(id: String): Task[Option[Post]] = l.debug(s"Selecting post for id $id") *> d.getById(id)
          }
      )
  }

}

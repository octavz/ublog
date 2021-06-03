import zio._
import zio.Task
import zio.logging._

import models.Post

object logic {
  type Logic = Has[Logic.Service]

  object Logic {
    trait Service {
      def createPost(post: Post): Task[Unit]
      def getPosts(): Task[List[Post]]
      def getPostById(id: String): Task[Option[Post]]
    }
  }

  def createPost(post: Post)  = ZIO.accessM[Logic](_.get.createPost(post))
  def getPosts()              = ZIO.accessM[Logic](_.get.getPosts())
  def getPostById(id: String) = ZIO.accessM[Logic](_.get.getPostById(id))

  def live: ZLayer[db.Db with Logging, Nothing, Logic] =
    ZLayer.fromServices[db.Db.Service, Logger[String], logic.Logic.Service](
      (d: db.Db.Service, l: Logger[String]) =>
        new Logic.Service {
          def createPost(post: Post): Task[Unit] = d.insert(post).unit
          def getPosts(): Task[List[Post]] = {
            l.log(LogLevel.Debug)("xyz") *> d.selectAll()
          }
          def getPostById(id: String): Task[Option[Post]] = {
            l.log(LogLevel.Debug)("abc") *> d.getById(id)
          }
        }
    )
}

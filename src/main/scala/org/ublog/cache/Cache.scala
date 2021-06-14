package org.ublog.cache

import org.ublog.models.Post
import org.ublog.serde._
import spray.json._
import zio._

object Cache {
  // service definition
  trait Service {
    def write(post: Post): Task[Unit]
    def read(id: String): Task[Option[Post]]
  }

  // accessors
  def write(post: Post): ZIO[Cache, Throwable, Unit]        = ZIO.accessM[Cache](_.get.write(post))
  def read(id: String): ZIO[Cache, Throwable, Option[Post]] = ZIO.accessM[Cache](_.get.read(id))

  // implementation
  val live: ZLayer[RedisOps, Nothing, Cache] =
    ZLayer.fromFunction { env: RedisOps =>
      new Service {
        def write(post: Post) = env.get.set(post.id, post.toJson.compactPrint)
        def read(id: String) =
          for {
            maybePostStr <- env.get.get(id)
            post         <- ZIO(maybePostStr.map(_.parseJson.convertTo[Post]))
          } yield post
      }
    }

}

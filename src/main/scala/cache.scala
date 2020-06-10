import zio._
import models._
import serde._
import spray.json._

object cache {

  type Cache = Has[Cache.Service]

  object Cache {
    trait Service {
      def write(post: Post): Task[Unit]
      def read(id: String): Task[Option[Post]]
    }
  }

  def write(post: Post) = ZIO.accessM[Cache](_.get.write(post))
  def read(id: String)  = ZIO.accessM[Cache](_.get.read(id))

  val live: ZLayer[redisOps.RedisOps, Nothing, Cache] =
    ZLayer.fromFunction { env: redisOps.RedisOps =>
      new Cache.Service {
        def write(post: Post) = env.get.set(post.id, post.toJson.compactPrint)
        def read(id: String) =
          for {
            maybePostStr <- env.get.get(id)
            post         <- ZIO(maybePostStr.map(_.parseJson.convertTo[models.Post]))
          } yield post
      }
    }
}

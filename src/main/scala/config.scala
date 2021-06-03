import zio._

object config {
  type Config = Has[Service]

  trait Service {
    val redisUri: String
  }

  def redisUri: RIO[Config, String] = ZIO.access(_.get.redisUri)

  val live: ZLayer[Any, Nothing, Config] = ZLayer.succeed(new Service {

    override val redisUri: String = "redis://localhost"

  })
}

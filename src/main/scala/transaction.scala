import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.duration.durationInt
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.gaelrenoux.tranzactio._
import io.github.gaelrenoux.tranzactio.doobie.Database

import javax.sql.DataSource

object transaction {

  val connectionPoolLayer: ZLayer[Blocking, Throwable, Has[DataSource]] =
    ZIO
      .accessM[Blocking] { _ =>
        blocking.effectBlocking {
          val config = new HikariConfig()
          config.setJdbcUrl("jdbc:postgresql://localhost:5432/postgres")
          config.setUsername("postgres")
          config.setPassword("postgres")
          new HikariDataSource(config)
        }
      }
      .toLayer

  val errorStrategiesLayer: ULayer[Has[ErrorStrategies]] =
    ZLayer.succeed(ErrorStrategies.timeout(10.seconds).retryCountFixed(3, 3.seconds))

  val dbLayer = (Blocking.live >>> connectionPoolLayer ++ errorStrategiesLayer ++ Clock.live ++ Blocking.live) >>>
    Database.fromDatasourceAndErrorStrategies

}

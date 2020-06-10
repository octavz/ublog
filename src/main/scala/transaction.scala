import zio.ZLayer
import com.zaxxer.hikari.HikariConfig
import zio.blocking.Blocking
import com.zaxxer.hikari.HikariDataSource
import zio.clock.Clock
import io.github.gaelrenoux.tranzactio.doobie.Database

object transaction {

  val config = new HikariConfig()
  config.setJdbcUrl("jdbc:postgresql://localhost:5432/postgres")
  config.setUsername("postgres")
  config.setPassword("postgres")
  val datasource: javax.sql.DataSource = new HikariDataSource(config)

  val dbLayer: ZLayer[Blocking with Clock, Nothing, Database] = Database.fromDatasource(datasource)

}

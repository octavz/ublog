package org.ublog

import kamon.Kamon
import org.ublog.web._
import zio._

object main extends App {

  override def run(args: List[String]): URIO[ZEnv, ExitCode] = {
    val ioKamon   = ZIO(Kamon.init())
    val ioMigrate = migration.migrateInternal().provideLayer(layers.migrationLayer)
    val ioZioHttp = Web.startServer().provideLayer(layers.zioHttpLayer)
    (ioKamon *> ioMigrate *> ioZioHttp).exitCode

//    val ioAkka = Web.startServer().provideLayer(layers.akkaWebLayer(this))
//    (ioKamon *> ioMigrate *> ioAkka).exitCode
  }
}

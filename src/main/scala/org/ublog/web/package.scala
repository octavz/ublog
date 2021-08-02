package org.ublog

import zio._

package object web {
  // service definition
  trait Web {
    def startServer(): ZIO[Any, Throwable, Nothing]
  }

  // accessors = public API for the service
  object Web {
    def startServer(): ZIO[Has[Web], Throwable, Nothing] = ZIO.serviceWith[Web](_.startServer())
  }
}

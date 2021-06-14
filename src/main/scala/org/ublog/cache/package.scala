package org.ublog

import dev.profunktor.redis4cats.RedisCommands
import zio._

package object cache {
  type RedisCmds = Has[RedisCommands[Task, String, String]]
  type RedisOps  = Has[RedisOps.Service]
  type Cache     = Has[Cache.Service]
}

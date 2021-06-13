package org.ublog

import io.scalaland.chimney.dsl._
import org.ublog.persistence.models.DbPost

object models {

  case class Post(id: String, title: String, content: String, author: String) {
    def toDb(): DbPost = this.transformInto[DbPost]
  }

  def fromDb(dbPost: DbPost): Post = dbPost.transformInto[Post]

}

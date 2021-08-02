package org.ublog.web

import io.scalaland.chimney.dsl._
import org.ublog.models.Post
import zio.json._

object models {

  case class WebPost(id: String, title: String, content: String, author: String) {
    def toPost: Post = this.transformInto[Post]
  }

  object WebPost {
    def apply(post: Post): WebPost = post.transformInto[WebPost]

    implicit val webPostEncoder: JsonEncoder[WebPost] = DeriveJsonEncoder.gen[WebPost]
    implicit val webPostDecoder: JsonDecoder[WebPost] = DeriveJsonDecoder.gen[WebPost]
  }
}

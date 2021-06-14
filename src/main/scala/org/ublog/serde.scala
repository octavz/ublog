package org.ublog

import org.ublog.models.Post
import spray.json.DefaultJsonProtocol._
import spray.json._

object serde {

  // used in cache and routes layers
  implicit val postFmt: RootJsonFormat[Post] = jsonFormat4(models.Post)
}

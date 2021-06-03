import spray.json._
import spray.json.DefaultJsonProtocol._

import models._

object serde {
  implicit val postFmt: RootJsonFormat[Post] = jsonFormat4(Post)
}

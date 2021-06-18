package org.ublog

import org.ublog.models.Post
import org.ublog.serde._
import spray.json._
import zio.test.Assertion._
import zio.test._

object SerDeSpec extends DefaultRunnableSpec {

  val postGen = for {
    id      <- Gen.anyString
    title   <- Gen.anyString
    content <- Gen.anyString
    author  <- Gen.anyString
  } yield Post(id, title, content, author)

  override def spec =
    suite("Model serialization")(
      suite("Post")(
        test("successful serialization") {
          val post     = Post("1", "Title", "content", "me")
          val postJson = post.toJson
          val expected =
            JsObject(
              "id" -> JsString(post.id),
              "title" -> JsString(post.title),
              "content" -> JsString(post.content),
              "author" -> JsString(post.author)
            )

          assert(postJson)(equalTo(expected))
        },
        testM("successful serialization with gen") {
          check(postGen) { post =>
            val postJson = post.toJson
            val expected =
              JsObject(
                "id" -> JsString(post.id),
                "title" -> JsString(post.title),
                "content" -> JsString(post.content),
                "author" -> JsString(post.author)
              )

            assert(postJson)(equalTo(expected))
          }
        }
      )
    )
}

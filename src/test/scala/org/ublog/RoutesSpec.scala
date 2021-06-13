package org.ublog

import org.ublog.data.Data
import org.ublog.logic.Logic
import org.ublog.models.Post
import org.ublog.serde._
import spray.json._
import zio._
import zio.test.Assertion._
import zio.test._

object RoutesSpec extends DefaultRunnableSpec {

  val postGen = for {
    id      <- Gen.anyString
    title   <- Gen.anyString
    content <- Gen.anyString
    author  <- Gen.anyString
  } yield Post(id, title, content, author)

  override def spec =
    suite("suite")(
      suite("ser")(
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
        },
        testM("getPostById") {
          val expected = Post("id", "title", "content", "author")
          val testDataServiceLayer = ZLayer.succeed(new Data.Service {
            def selectAll(): Task[List[Post]]                 = Task.succeed(Nil)
            def insert(post: Post): ZIO[Any, Throwable, Unit] = Task.unit
            def getById(id: String): Task[Option[Post]]       = Task.some(expected)
          })
          val liveLogic: ZLayer[Any, Nothing, Logic] = (testDataServiceLayer ++ layers.loggerLayer) >>> Logic.live
          val result                                 = Logic.getPostById("id").provideLayer(liveLogic)
          assertM(result)(equalTo(Some(expected)))
        }
      )
    )
}

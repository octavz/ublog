import zio._
import zio.test._
import zio.test.Assertion._
import serde._
import spray.json._

import db._
import logic._
import models._

object spec extends DefaultRunnableSpec {

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
          val testDbL = ZLayer.succeed(new Db.Service {
            def selectAll(): Task[List[Post]]                = Task.succeed(Nil)
            def insert(post: Post): ZIO[Any, Throwable, Int] = Task.succeed(1)
            def getById(id: String): Task[Option[Post]]      = Task.some(expected)
          })
          val liveLogic: ZLayer[Any, Nothing, Logic] = (testDbL ++ layers.logger) >>> logic.live
          val result                                 = getPostById("id").provideLayer(liveLogic)
          assertM(result)(equalTo(Some(expected)))
        }
      )
    )
}

package org.ublog

import org.ublog.data.Data
import org.ublog.logic.{Logic, LogicLive}
import org.ublog.metrics.MetricsLive
import org.ublog.models.Post
import org.ublog.stubs._
import zio._
import zio.test.Assertion.equalTo
import zio.test._

object LogicSpec extends DefaultRunnableSpec {

  def stubDataLayer(value: Ref[Int]) =
    (layers.loggerLayer ++ ZLayer.succeed(stubDataService(value)) ++ MetricsLive.layer) >>> LogicLive.layer

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("Logic")(
      suite("CRUD")(
        testM("Create post") {
          val post = Post(id = "id", title = "Title", content = "bla", author = "author")

          for {
            number <- ZRef.make(0)
            layer = stubDataLayer(number)
            _      <- Logic.createPost(post).provideLayer(layer)
            result <- number.get
          } yield assert(result)(equalTo(1))
        },
        testM("Get post by id") {

          val expected = Post("id", "title", "content", "author")
          val testDataServiceLayer = ZLayer.succeed(new Data {
            def selectAll(): Task[List[Post]]                 = Task.succeed(Nil)
            def insert(post: Post): ZIO[Any, Throwable, Unit] = Task.unit
            def getById(id: String): Task[Option[Post]]       = Task.some(expected)
          })
          val liveLogic = (testDataServiceLayer ++ layers.loggerLayer ++ MetricsLive.layer) >>> LogicLive.layer
          val result    = Logic.getPostById("id").provideLayer(liveLogic)
          assertM(result)(equalTo(Some(expected)))
        }
      )
    )
}

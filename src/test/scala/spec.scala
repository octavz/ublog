import zio.test._
import zio.test.Assertion._
import config._

object spec extends DefaultRunnableSpec {

  override def spec =
    suite("suite")(
      test("test 1") {
        assertCompletes
      }
    )
}

package akka.http.interop

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import zio._
import zio.test.Assertion._
import zio.test._

object ZIOInteropSpec extends ZIORouteTest {

  object SimpleRoutes extends ZIOSupport {
    val routes = pathPrefix("task") {
      get {
        val res: Task[String] = ZIO.fail(new Throwable("error"))
        complete(res)
      }
    } ~ pathPrefix("uio") {
      get {
        // todo: when typed as UIO it fails to compile for Scala 2.12.11
        val res: Task[String] = ZIO.succeed("OK")
        complete(res)
      }
    }
  }

  val domainRoutes = Route.seal(Api.routes)
  val simpleRoutes = Route.seal(SimpleRoutes.routes)

  private val specs: Spec[Any, TestFailure[Throwable], TestSuccess] =
    suite("ZIO Interop routes")(
      test("succeed on /ok") {
        ZIO.attempt(Get("/ok") ~> domainRoutes ~> check {
          val s = status
          assert(s)(equalTo(StatusCodes.OK))
        })
      },
      test("fail with 500 on /internal_server_error") {
        ZIO.attempt(Get("/internal_server_error") ~> domainRoutes ~> check {
          val s = status
          assert(s)(equalTo(StatusCodes.InternalServerError))
        })
      },
      test("fail with 400 on /bad_request") {
        ZIO.attempt(Get("/bad_request") ~> domainRoutes ~> check {
          val s = status
          assert(s)(equalTo(StatusCodes.BadRequest))
        })
      },
      test("succeed fail with 500 on /task (no domain errors)") {
        ZIO.attempt(Get("/task") ~> simpleRoutes ~> check {
          val s = status
          assert(s)(equalTo(StatusCodes.InternalServerError))
        })
      },
      test("succeed on /uio (no domain errors)") {
        ZIO.attempt(Get("/uio") ~> simpleRoutes ~> check {
          val s = status
          assert(s)(equalTo(StatusCodes.OK))
        })
      }
    )

  def spec = specs
}

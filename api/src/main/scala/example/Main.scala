package example

import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.HttpRoutes
import org.http4s.dsl.impl.Root
import org.http4s.dsl.io._
import cats.implicits._
import doobie.util.transactor.Transactor
import doobie.util.transactor.Transactor.Aux
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS

object Main extends IOApp {

  val dbSettings = DbSettings("org.postgresql.Driver", "jdbc:postgresql://postgres:5432/reviews", "postgres", "secret")

  def xa: Aux[IO, Unit] = Transactor.fromDriverManager[IO](
    dbSettings.driver,
    dbSettings.url,
    dbSettings.user,
    dbSettings.password
  )

  private val corsConfig = {
    val default = CORS.DefaultCORSConfig
    default.copy(allowedOrigins = _ => true, allowedMethods = Set("*").some)
  }

  private val db = new Database(xa)

  object OptionalIntQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Int]("num-reviews")

  private val summaryService = HttpRoutes
    .of[IO] {
      case GET -> Root / "summaries" :? OptionalIntQueryParamMatcher(maybeInt) =>
        maybeInt match {
          case Some(numReviews) => Ok(db.getSummaries(numReviews).map(Summaries.fromReviewSummaries))
          case None             => Ok(db.getSummaries(0).map(Summaries.fromReviewSummaries))
        }

    }
    .orNotFound

  def run(args: List[String]): IO[ExitCode] =
    for {
      _ <- BlazeServerBuilder[IO]
        .withHttpApp(CORS(summaryService, corsConfig))
        .bindHttp(8080, "0.0.0.0")
        .serve
        .compile
        .drain
    } yield ExitCode.Success
}

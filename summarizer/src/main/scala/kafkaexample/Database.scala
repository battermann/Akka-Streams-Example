package kafkaexample

import cats.effect.{ContextShift, IO, Timer}
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.util.transactor.Transactor.Aux
import newts.{Max, Min}
import org.flywaydb.core.Flyway
import retry.CatsEffect._
import retry.RetryDetails.{GivingUp, WillDelayAndRetry}
import retry.{RetryPolicies, _}
import cats.implicits._
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._

final case class DbSettings(driver: String, url: String, user: String, password: String)

trait Database {
  def migrateWithRetries: Int
  def upsertReviewSummary(reviewSummary: ReviewSummary): IO[Unit]
}

class PostgresDb(dbSettings: DbSettings) extends Database {

  private implicit val contextShift: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.global)

  private val xa: Aux[IO, Unit] = Transactor.fromDriverManager[IO](
    driver = dbSettings.driver,
    url = dbSettings.url,
    user = dbSettings.user,
    pass = dbSettings.password
  )

  def migrateWithRetries: Int = {
    implicit val timer: Timer[IO] = IO.timer(global)

    val retryPolicy = RetryPolicies.capDelay[IO](5.minutes, RetryPolicies.constantDelay(10.second))

    def logError(err: Throwable, details: RetryDetails): IO[Unit] = details match {
      case WillDelayAndRetry(_: FiniteDuration, retriesSoFar: Int, _: FiniteDuration) =>
        IO(println(s"Failed to migrate. So far we have retried $retriesSoFar times. Error message: ${err.getMessage}"))
      case GivingUp(totalRetries: Int, _: FiniteDuration) =>
        IO(println(s"Giving up after $totalRetries retries"))
    }

    val migrate = IO {
      val flyway = Flyway
        .configure()
        .dataSource(dbSettings.url, dbSettings.user, dbSettings.password)
        .load()

      flyway.migrate()
    }

    val result: IO[Int] = retryingOnAllErrors[Int](
      policy = retryPolicy,
      onError = logError
    )(migrate)

    result.unsafeRunSync()
  }

  def upsertReviewSummary(reviewSummary: ReviewSummary): IO[Unit] = upsertReviewSummaryConnectionIO(reviewSummary).transact(xa)

  private def getReviewSummaryConnectionIO(asin: String): ConnectionIO[Option[ReviewSummary]] =
    sql"SELECT id, count, sum, avg, min, max FROM review_summaries WHERE id = $asin"
      .query[(String, Int, Double, Double, Double, Double)]
      .map { case (id, count, sum, avg, min, max) => ReviewSummary(id, count, sum, avg, Min(min), Max(max)) }
      .option

  private def upsertReviewSummaryConnectionIO(reviewSummary: ReviewSummary): ConnectionIO[Unit] =
    getReviewSummaryConnectionIO(reviewSummary.id)
      .flatMap {
        case None => insertReviewSummaryConnectionIO(reviewSummary)
        case Some(existingSummary) =>
          val combined = existingSummary |+| reviewSummary
          sql"""UPDATE review_summaries
               |SET count = ${combined.count}, sum = ${combined.sum}, avg = ${combined.avg}, min = ${combined.min.getMin}, max = ${combined.max.getMax}
               |WHERE id = ${combined.id}""".stripMargin.update.withGeneratedKeys("id").compile.lastOrError
      }

  private def insertReviewSummaryConnectionIO(reviewSummary: ReviewSummary): ConnectionIO[Unit] =
    sql"""INSERT INTO review_summaries (id, count, sum, avg, min, max)
         |VALUES (${reviewSummary.id}, ${reviewSummary.count}, ${reviewSummary.sum}, ${reviewSummary.avg}, ${reviewSummary.min.getMin}, ${reviewSummary.max.getMax})""".stripMargin.update.run
      .map(_ => ())
}

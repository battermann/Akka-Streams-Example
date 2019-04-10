package example

import cats.effect.IO
import io.circe.generic.auto._
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

final case class ReviewSummary(id: String, count: Int, avg: Double, min: Double, max: Double)

object ReviewSummary {
  implicit val entityDecoder: EntityEncoder[IO, ReviewSummary]           = jsonEncoderOf[IO, ReviewSummary]
  implicit val entityListDecoder: EntityEncoder[IO, List[ReviewSummary]] = jsonEncoderOf[IO, List[ReviewSummary]]
}

final case class Summaries(totalProducts: Int, totalReviews: Int, reviewSummaries: List[ReviewSummary])

object Summaries {
  def fromReviewSummaries(reviewSummaries: List[ReviewSummary]): Summaries = {
    Summaries(
      reviewSummaries.size,
      reviewSummaries.map(_.count).sum,
      reviewSummaries
    )
  }

  implicit val entityDecoder: EntityEncoder[IO, Summaries] = jsonEncoderOf[IO, Summaries]
}

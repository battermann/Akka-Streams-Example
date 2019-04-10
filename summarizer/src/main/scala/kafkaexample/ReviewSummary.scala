package kafkaexample

import cats.implicits._
import cats.kernel.Semigroup
import io.circe.{Decoder, HCursor}
import newts._

final case class ReviewSummary(id: String, count: Int, sum: Double, avg: Double, min: Min[Double], max: Max[Double])

object ReviewSummary {
  implicit val decoder: Decoder[ReviewSummary] =
    (c: HCursor) =>
      for {
        asin    <- c.downField("asin").as[String]
        overall <- c.downField("overall").as[Double]
      } yield ReviewSummary(asin, 1, overall, overall, Min(overall), Max(overall))

  implicit val semiGroup: Semigroup[ReviewSummary] = (x: ReviewSummary, y: ReviewSummary) => {
    val count = x.count |+| y.count
    val sum   = x.sum |+| y.sum
    val avg   = sum / count
    ReviewSummary(x.id, count, sum, avg, x.min |+| y.min, x.max |+| y.max)
  }
}

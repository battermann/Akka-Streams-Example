package example

import cats.effect.IO
import doobie.util.transactor.Transactor.Aux
import doobie.implicits._

final case class DbSettings(driver: String, url: String, user: String, password: String)

class Database(xa: Aux[IO, Unit]) {
  def getSummaries(numReviews: Int): IO[List[ReviewSummary]] =
    sql"SELECT id, count, avg, min, max FROM review_summaries WHERE count >= $numReviews ORDER BY avg DESC, count DESC"
      .query[ReviewSummary]
      .to[List]
      .transact(xa)
}

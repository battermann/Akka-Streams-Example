package kafkaexample

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import kafkaexample.StreamComponents._

object Main extends App {

  // first create or migrate DB
  val dbSettings = DbSettings("org.postgresql.Driver", "jdbc:postgresql://postgres:5432/reviews", "postgres", "secret")
  val db         = new PostgresDb(dbSettings)

  db.migrateWithRetries

  // Create and run stream
  implicit val system: ActorSystem             = ActorSystem("kafka-producer")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  kafkaSource
    .map(_.value)
    .via(decodeLines)
    .mapAsync(1)(upsertSummary(db))
    .runWith(Sink.ignore)
}

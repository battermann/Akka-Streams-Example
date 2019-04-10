package kafkaexample

import akka.NotUsed
import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer
import akka.stream.scaladsl.{Flow, Source}
import io.circe.parser.decode
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.common.serialization.StringDeserializer

import scala.concurrent.Future

object StreamComponents {
  def kafkaSource(implicit system: ActorSystem): Source[ConsumerRecord[String, String], Consumer.Control] = {
    val bootstrapServers =
      "kafka01.internal-service:9092,kafka01.internal-service:9093,kafka01.internal-service:9094"

    val config = system.settings.config.getConfig("akka.kafka.consumer")

    val consumerSettings =
      ConsumerSettings(config, new StringDeserializer, new StringDeserializer)
        .withBootstrapServers(bootstrapServers)
        .withGroupId("summarizer")
        .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

    Consumer
      .atMostOnceSource(consumerSettings, Subscriptions.topics("reviews"))
  }

  def decodeLines: Flow[String, ReviewSummary, NotUsed] =
    Flow[String]
      .map(line => decode[ReviewSummary](line).toOption)
      .mapConcat(_.toList)

  def upsertSummary(database: Database)(summary: ReviewSummary): Future[Unit] = {
    database.upsertReviewSummary(summary).unsafeToFuture()
  }
}

package kafkaexample

import java.nio.file.Paths

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Flow, Framing, Sink, Source}
import akka.util.ByteString
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.{ExecutionContext, Future}

object StreamComponents {
  def readLines(path: String): Source[String, Future[IOResult]] =
    FileIO
      .fromPath(Paths.get(path))
      .via(Framing.delimiter(ByteString("\n"), Int.MaxValue, false))
      .map(_.utf8String)

  def decodeLines(ec: ExecutionContext): Flow[String, Review, NotUsed] =
    Flow[String]
      .grouped(10000)
      .mapAsyncUnordered(8) { lines =>
        Future(lines.flatMap(line => decode[Review](line).toOption))(ec)
      }
      .mapConcat(_.toList)

  def toProducerRecord(topic: String): Flow[Review, ProducerRecord[String, String], NotUsed] =
    Flow[Review]
      .map(_.asJson.noSpaces)
      .map(value => new ProducerRecord[String, String](topic, value))

  def kafkaSink(implicit system: ActorSystem): Sink[ProducerRecord[String, String], Future[Done]] = {
    val bootstrapServers =
      "kafka01.internal-service:9092,kafka01.internal-service:9093,kafka01.internal-service:9094"

    val producerSettings =
      ProducerSettings(system, new StringSerializer, new StringSerializer)
        .withBootstrapServers(bootstrapServers)

    Producer.plainSink[String, String](producerSettings)
  }
}

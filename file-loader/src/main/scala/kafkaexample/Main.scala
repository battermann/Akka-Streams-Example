package kafkaexample

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import kafkaexample.StreamComponents._
import scala.concurrent.duration._
import scala.reflect.io.File
import scala.util.{Failure, Success}

object Main extends App {
  val maybePath: Option[String] = args.headOption.filter(File(_).exists)

  implicit val system: ActorSystem             = ActorSystem("kafka-producer")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  def run(path: String): Unit = {
    readLines(path)
      .via(decodeLines(system.dispatcher))
      .via(toProducerRecord("reviews"))
      .throttle(20, 1.second)
      .runWith(kafkaSink)
      .onComplete {
        case Success(_) =>
          println("Stream completed.")
          system.terminate()
        case Failure(err) =>
          println("Stream failed.")
          println(err.getMessage)
          System.exit(1)
      }(system.dispatcher)
  }

  maybePath match {
    case None =>
      println("File not found.")
      System.exit(1)
    case Some(path) =>
      run(path)
  }
}

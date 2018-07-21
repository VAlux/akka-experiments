package experiments

import akka.NotUsed
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Source
import akka.util.ByteString
import webserver.WebServer

import scala.util.Random

object StreamingExample {

  val randomStream: Source[Int, NotUsed] =
    Source.fromIterator(() => Iterator.continually(Random.nextInt()))

  val randomRoute: Route = {
    path(pm = "random") {
      get {
        complete(
          HttpEntity(
            ContentTypes.`text/plain(UTF-8)`,
            randomStream.map(n => ByteString(s"$n\n"))))
      }
    }
  }

  def main(args: Array[String]): Unit = {
    val server = new WebServer(interface = "localhost", port = 8080, randomRoute)
    server.start()
  }
}

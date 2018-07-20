package experiments

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.Future
import scala.util.{Failure, Success}
import webserver.WebServer._

object HttpClient {

  def main(args: Array[String]): Unit = {
    val responseFuture: Future[HttpResponse] =
      Http().singleRequest(HttpRequest(uri = "https://akka.io"))

    responseFuture.onComplete {
      case Success(response) => println(response)
      case Failure(_) => sys.error("something wrong")
    }
  }
}

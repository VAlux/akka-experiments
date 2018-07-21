package experiments

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import webserver.WebServer
import webserver.WebServer._

object LowLevelHttpApi {

  val requestHandler: HttpRequest => HttpResponse = {
    case HttpRequest(GET, Uri.Path("/"), _, _, _) =>
      HttpResponse(entity = HttpEntity(
        ContentTypes.`text/plain(UTF-8)`,
        "<html><body>Hello low level akka http api</body></html>"))

    case HttpRequest(GET, Uri.Path("/ping"), _, _, _) =>
      HttpResponse(entity = "Pong!")

    case r: HttpRequest =>
      r.discardEntityBytes()
      HttpResponse(StatusCodes.NotFound, entity = "Unknown resource")
  }

  def main(args: Array[String]): Unit = {
    val server = new WebServer(interface = "localhost", port = 8080, requestHandler)
    server.start()
  }
}

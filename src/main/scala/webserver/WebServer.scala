package webserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.StdIn

import WebServer._

class WebServer(protected val interface: String, protected val port: Int) {

  val bindingFuture: Future[Http.ServerBinding] = Future.never

  def start(): Unit = {
    println(s"Server started on http://$interface:$port/\npress RETURN to terminate...")
    StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => actorSystem.terminate())
  }
}

class RoutedWebServer(override val interface: String, override val port: Int)
                     (implicit val route: Route) extends WebServer(interface, port) {

  override val bindingFuture: Future[Http.ServerBinding] =
    Http().bindAndHandle(route, interface, port)
}

class AsyncWebServer(override val interface: String, override val port: Int)
                   (implicit val requestHandler: HttpRequest => Future[HttpResponse]) extends WebServer(interface, port) {

  override val bindingFuture: Future[Http.ServerBinding] =
    Http().bindAndHandleAsync(requestHandler, interface, port)
}

class SyncWebServer(override val interface: String, override val port: Int)
                   (implicit val requestHandler: HttpRequest => HttpResponse) extends WebServer(interface, port) {

  override val bindingFuture: Future[Http.ServerBinding] =
    Http().bindAndHandleSync(requestHandler, interface, port)
}

object WebServer {
  implicit val actorSystem: ActorSystem = ActorSystem("web-server")
  implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher
}

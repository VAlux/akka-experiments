package webserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import webserver.WebServer._

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.StdIn

sealed trait BindingHandlerProvider[BindingType] {
  def create(interface: String, port: Int, binding: BindingType): Future[Http.ServerBinding]
}

object BindingHandlerProviderInstances {

  type AsyncHandler = HttpRequest => Future[HttpResponse]
  type SyncHandler = HttpRequest => HttpResponse

  implicit val routeBindingHandler: BindingHandlerProvider[Route] = new BindingHandlerProvider[Route] {
    override def create(interface: String, port: Int, binding: Route): Future[Http.ServerBinding] =
      Http().bindAndHandle(binding, interface, port)
  }

  implicit val syncBindingHandler: BindingHandlerProvider[SyncHandler] = new BindingHandlerProvider[SyncHandler] {
    override def create(interface: String, port: Int, binding: SyncHandler): Future[Http.ServerBinding] =
      Http().bindAndHandleSync(binding, interface, port)
  }

  implicit val asyncBindingHandler: BindingHandlerProvider[AsyncHandler] = new BindingHandlerProvider[AsyncHandler] {
    override def create(interface: String, port: Int, binding: AsyncHandler): Future[Http.ServerBinding] =
      Http().bindAndHandleAsync(binding, interface, port)
  }
}

class WebServer[A: BindingHandlerProvider](val interface: String, val port: Int, binding: A) {
  val bindingFuture: Future[Http.ServerBinding] =
    implicitly[BindingHandlerProvider[A]].create(interface, port, binding)

  def start(): Unit = {
    println(s"Server started on http://$interface:$port/\npress RETURN to terminate...")
    StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => actorSystem.terminate())
  }
}

object WebServer {
  implicit val actorSystem: ActorSystem = ActorSystem("web-server")
  implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher
}

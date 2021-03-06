package experiments.websocket_chat

import akka.NotUsed
import akka.event.Logging
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, MergeHub}
import webserver.WebServer
import webserver.WebServer._

import scala.concurrent.Future

object WebsocketChatExample {

  val (chatSink, chatSource) = MergeHub.source[String].toMat(BroadcastHub.sink[String])(Keep.both).run()

  val websocketLoggingAdapter = Logging(actorSystem, "Websocket chat")

  val userFlow: Flow[Message, Message, NotUsed] =
    Flow[Message].mapAsync(parallelism = 1) {
      case TextMessage.Strict(content) => Future.successful(content)
      case streamed: TextMessage.Streamed => streamed.textStream.runFold("")(_ ++ _)
      case _ => Future.never
    }.via(Flow.fromSinkAndSource(chatSink, chatSource))
      .map[Message](content => TextMessage(content))

  val echoRoute: Route = {
    path("hello") {
      get {
        complete(HttpEntity("Hello akka - http world"))
      }
    }
  }

  val websocketChatRoute: Route = {
    path(pm = "chat") {
      get {
        withLog(websocketLoggingAdapter) {
          extractLog { implicit log =>
            log.info("start handling websocket messages...")
            handleWebSocketMessages(userFlow)
          }
        }
      }
    }
  }

  def main(args: Array[String]): Unit = {
    import webserver.BindingHandlerProviderInstances._

    val route = echoRoute ~ websocketChatRoute
    val server = new WebServer("localhost", 8080, route)
    server.start()
  }
}

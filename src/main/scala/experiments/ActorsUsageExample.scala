package experiments

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.http.scaladsl.model.StatusCodes
import akka.pattern.ask
import spray.json.DefaultJsonProtocol._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import spray.json.RootJsonFormat
import webserver.WebServer

import scala.concurrent.Future
import scala.concurrent.duration._
import webserver.WebServer._

object ActorsUsageExample {

  case class Bid(userId: String, offer: Int)
  case object GetBids
  case class Bids(bids: List[Bid])

  class Auction extends Actor with ActorLogging {
    var bids = List.empty[Bid]

    override def receive: Receive = {
      case bid @ Bid(userId, offer) =>
        bids = bids :+ bid
        log.info(s"Bid complete: $userId, $offer")
      case GetBids => sender() ! Bids(bids)
      case _ => log.error("Invalid message.")
    }
  }

  implicit val bidFormat: RootJsonFormat[Bid] = jsonFormat2(Bid)
  implicit val bidsFormat: RootJsonFormat[Bids] = jsonFormat1(Bids)

  val auction: ActorRef = actorSystem.actorOf(Props[Auction], name = "auction")

  val route: Route = {
    path(pm = "auction") {
      put {
        parameter("bid".as[Int], "user") { (bid, user) =>
          auction ! Bid(user, bid)
          complete((StatusCodes.Accepted, "Bid Placed"))
        }
      } ~
      get {
        implicit val timeout: Timeout = 5.seconds
        val bids: Future[Bids] = (auction ? GetBids).mapTo[Bids]
        complete(bids)
      }
    }
  }

  def main(args: Array[String]): Unit = {
    import webserver.BindingHandlerProviderInstances._

    val server = new WebServer(interface = "localhost", port = 8080, route)
    server.start()
  }
}

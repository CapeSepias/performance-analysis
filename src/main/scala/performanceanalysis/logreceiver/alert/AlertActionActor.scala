package performanceanalysis.logreceiver.alert

import scala.concurrent.Future
import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.{Http, HttpExt}
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import performanceanalysis.server.messages.AlertMessages.AlertRuleViolated

object AlertActionActor {

  def props(): Props = Props.apply(new AlertActionActor)
}

class AlertActionActor extends Actor with ActorLogging {

  implicit val materializer = ActorMaterializer()

  lazy val http: HttpExt = Http(context.system)

  def receive: Receive = {
    case AlertRuleViolated(endpoint, message) => alert(endpoint, message)
  }

  def alert(endpoint:String, message:String): Unit = {
    //validate endpoint?
    log.info(s"sending alert to $endpoint")
    val req: HttpRequest = HttpRequest(method = HttpMethods.POST, uri = endpoint)
    val resp: Future[HttpResponse] = http.singleRequest(req)
    //check http status of response
    //possibly retry
  }
}

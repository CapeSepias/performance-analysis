package performanceanalysis.administrator

import akka.actor._
import akka.pattern.{ask, pipe}
import performanceanalysis.server.messages.AdministratorMessages._
import performanceanalysis.server.messages.AlertMessages._
import performanceanalysis.server.messages.LogMessages._

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by m06f791 on 24-3-2016.
  */

object AdministratorActor {

  def props(logReceiverActor: ActorRef): Props = Props(new AdministratorActor(logReceiverActor))
}

class AdministratorActor(logReceiverActor: ActorRef) extends Actor with ActorLogging with LogParserActorCreater {
  this: LogParserActorCreater =>

  def receive: Receive = normal(Map.empty[String, ActorRef])

  def normal(logParserActors: Map[String, ActorRef]): Receive = {
    case RegisterComponent(componentId, dateFormat) =>
      handleRegisterComponent(logParserActors, componentId, dateFormat, sender)
    case GetDetails(componentId) =>
      handleGetDetails(logParserActors, componentId, sender)
    case GetRegisteredComponents =>
      handleGetComponents(logParserActors, sender)
    case GetComponentLogLines(componentId) =>
      handleGetComponentLogLines(logParserActors, componentId, sender)
    case GetParsedLogLines(componentId, metricKey) =>
      handleGetParsedLogLines(logParserActors, componentId, metricKey, sender)
    case RegisterMetric(componentId, metric) =>
      handleRegisterMetric(logParserActors, componentId, metric, sender)
    case msg:RegisterAlertRule =>
      handleRegisterAlertingRule(logParserActors, msg);
    case msg:DeleteAllAlertingRules =>
      handleDeleteAlertingRules(logParserActors, msg)
    case msg:GetAlertRules =>
      handleGetAlertRules(logParserActors, msg)
  }

  private def handleRegisterComponent(logParserActors: Map[String, ActorRef], componentId: String, dateFormat: Option[String], sender: ActorRef) = {
    logParserActors.get(componentId) match {
      case None =>
        val newActor = createLogParserActor(context, componentId, dateFormat)
        // Notify LogReceiver of new actor
        logReceiverActor ! RegisterNewLogParser(componentId, newActor, None)
        // Update actor state
        log.debug("Created new component {}", componentId)
        val newLogParserActors = logParserActors.updated(componentId, newActor)
        context.become(normal(newLogParserActors))
        // Respond to sender
        sender ! LogParserCreated(componentId)
      case Some(ref) =>
        log.debug("Actor with component {} already existed", componentId)
        sender ! LogParserExisted(componentId)
    }
  }

  private def handleGetComponents(logParserActors: Map[String, ActorRef], sender: ActorRef) = {
    sender ! RegisteredComponents(logParserActors.keySet)
  }

  private def routeToLogParser(logParserActors: Map[String, ActorRef], componentId: String, sender: ActorRef)(action: ActorRef => Unit) = {
    logParserActors.get(componentId) match {
      case None => sender ! LogParserNotFound(componentId)
      case Some(ref) => action(ref)
    }
  }

  private def handleGetDetails(logParserActors: Map[String, ActorRef], componentId: String, sender: ActorRef) = {
    routeToLogParser(logParserActors, componentId, sender) { ref =>
      log.debug("Requesting details from {}", ref.path)
      (ref ? RequestDetails) pipeTo sender
    }
  }

  private def handleGetComponentLogLines(logParserActors: Map[String, ActorRef],
                                          componentId: String,
                                          sender: ActorRef) = {
    routeToLogParser(logParserActors, componentId, sender) { ref =>
      log.debug(s"Requesting logLines for $componentId from ${ref.path}")
      (ref ? RequestComponentLogLines) pipeTo sender
    }
  }

  private def handleGetParsedLogLines(logParserActors: Map[String, ActorRef],
                                      componentId: String,
                                      metricKey: String,
                                      sender: ActorRef) = {
    routeToLogParser(logParserActors, componentId, sender) { ref =>
      log.debug(s"Requesting parsed logLines for $componentId and $metricKey from ${ref.path}")
      (ref ? RequestParsedLogLines(metricKey)) pipeTo sender
    }
  }

  private def handleGetAlertRules(logParserActors: Map[String, ActorRef], msg: GetAlertRules) = {
    routeToLogParser(logParserActors, msg.componentId, sender()) { ref =>
      log.debug("Requesting alert rules from {}", ref.path)
      (ref ? RequestAlertRules(msg.metricKey)) pipeTo sender()
    }
  }

  private def handleRegisterMetric(logParserActors: Map[String, ActorRef], componentId: String, metric: Metric, sender: ActorRef) = {
    routeToLogParser(logParserActors, componentId, sender) { ref =>
        log.debug(s"Sending metric registration to {}", ref.path)
        (ref ? metric) pipeTo sender
    }
  }

  private def handleRegisterAlertingRule(logParserActors: Map[String, ActorRef], msg: RegisterAlertRule) = {
    routeToLogParser(logParserActors, msg.componentId, sender()) { ref =>
      log.debug("Sending new alerting rule to {}", ref.path)
      (ref ? msg) pipeTo sender()
    }
  }

  private def handleDeleteAlertingRules(logParserActors: Map[String, ActorRef], msg: DeleteAllAlertingRules) = {
    routeToLogParser(logParserActors, msg.componentId, sender()) { ref =>
      log.debug("Sending rule deletion message to {}", ref.path)
      (ref ? msg) pipeTo sender()
    }
  }
}

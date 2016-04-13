package performanceanalysis.server

import akka.actor.ActorRef
import performanceanalysis.server.Protocol._
import spray.json.DefaultJsonProtocol

object Protocol {

  /**
    * Used by Administrator towards AdministratorActor to register a new component
    */
  case class RegisterComponent(componentId: String)

  /**
    * Used by AdministratorActor towards Administrator to signal that a new component was registered
    */
  case class LogParserCreated(componentId: String)

  /**
    * Used by AdministratorActor towards Administrator to signal that the component was already registered
    */
  case class LogParserExisted(componentId: String)

  /**
    * Used by AdministratorActor towards Administrator and by LogReceiverActor towards LogReceiver
    * to signal that the component could not be found
    */
  case class LogParserNotFound(componentId: String)

  /**
    * Used by Administrator towards AdministratorActor to request details of a component
    */
  case class GetDetails(componentId: String)

  /**
    * Used by Administrator towards AdministratorActor to request a list of all registered components
    */
  case object GetRegisteredComponents

  /**
    * Used by AdministratorActor towards Administrator to return a list of all registered components
    */
  case class RegisteredComponents(componentIds: Set[String])

  /**
    * Used by LogReceiverActor towards LogReceiver to request processing of logs of a component
    */
  case class SubmitLogs(componentId: String, logs: String)

  /**
    * Used by AdministratorActor towards LogParserActor to request its details
    */
  case object RequestDetails

  /**
    * Used by LogParserActor towards AdministratorActor to return its details
    */
  case class Details(componentId: String)

  /**
    * Used by Administrator towards LogReceiver to notify it ofa new LogReceiver actor
    */
  case class RegisterNewLogParser(componentId: String, actor: ActorRef)

}

trait Protocol extends DefaultJsonProtocol {
  implicit val detailsFormatter = jsonFormat1(Details.apply)
  implicit val registerComponentsFormatter = jsonFormat1(RegisterComponent.apply)
  implicit val registeredComponentsFormatter = jsonFormat1(RegisteredComponents.apply)
}
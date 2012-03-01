import rice.environment.Environment
import rice.p2p.commonapi.{Id, Message}
import rice.pastry.commonapi.PastryIdFactory

/**
 * Created by IntelliJ IDEA.
 * User: beiske
 * Date: 22.02.12
 * Time: 20:14
 * To change this template use File | Settings | File Templates.
 */


abstract class MessageToUserResponsible(val user:UserID) extends Message{
  override def getPriority :Int = {
    Message.LOW_PRIORITY
  }
}

case class UserID(nick :String, network :String) {
  def getPastryId() : Id = {
    Factories.idFactory.buildId(nick + "@" + network)
  }
}

class Log()

case class RequestHostingStart(override val user :UserID, log : Option[Log], channels :Set[String]) extends MessageToUserResponsible(user)  {

}


case class RequestHostingEnd(override val user :UserID) extends MessageToUserResponsible(user)

object Factories {
  val environment = new Environment()
  val idFactory = new PastryIdFactory(environment)

}
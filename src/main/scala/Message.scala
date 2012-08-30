import java.net.{InetAddress, InetSocketAddress}
import java.util
import rice.environment.Environment
import rice.p2p.commonapi.Message
import rice.pastry.Id
import rice.pastry.commonapi.PastryIdFactory
import rice.pastry.socket.SocketPastryNodeFactory
import rice.pastry.standard.RandomNodeIdFactory
import rice.pastry.{NodeIdFactory, PastryNodeFactory, PastryNode}

/**
 * Created by IntelliJ IDEA.
 * User: beiske
 * Date: 22.02.12
 * Time: 20:14
 * To change this template use File | Settings | File Templates.
 */


abstract class MessageToUserResponsible(val user:UserID) extends Message{
  override def getPriority :Int = {
    Message.DEFAULT_PRIORITY
  }
}

case class UserID(nick :String, network :String) {
  def getPastryId() : Id = {
    Factories.idFactory.buildId(nick + "@" + network).asInstanceOf[Id]
  }
}

class Log() {
  var channelEvents :Map[String, List[LogMessage]] = Map()

  def merge(other : Log) {
    //val result = new Log()
    //result.
    channelEvents = mergeMap(List(other.channelEvents, channelEvents))(mergeLogMessages(_, _))
    //result
  }

  private def mergeLogMessages(a: List[LogMessage], b: List[LogMessage]) : List[LogMessage] = {
    b match {
      case Nil => a
      case _ =>
        a match {
          case Nil => b
          case x::xs =>
            if (x.date.before(b.head.date))
              x::mergeLogMessages(xs, b)
            else
              b.head::mergeLogMessages(a, b.tail)
        }
  }
  }

  def message(channel: String, nick: String, message: String) {
    channelEvents += channel -> (LogMessage(nick, message) :: channelEvents.getOrElse(channel, List()))
  }



  def mergeMap[A, B](ms: List[Map[A, B]])(f: (B, B) => B): Map[A, B] = {
    (Map[A, B]() /: (for (m <- ms; kv <- m) yield kv)) {
      (a, kv) =>
        a + (if (a.contains(kv._1)) kv._1 -> f(a(kv._1), kv._2) else kv)
    }
  }
}


case class LogMessage(nick: String, message:String) {
  val date = new util.Date()
}

case class HeartBeat(logs: Map[UserID, Log]) extends Message {
  override def getPriority :Int = {
    Message.LOW_PRIORITY
  }
}



case class RequestHostingStart(override val user :UserID, log : Option[Log], channels :Set[String]) extends MessageToUserResponsible(user)  {

}

case class RequestTransfer(id : Id)

case class RequestHostingEnd(override val user :UserID) extends MessageToUserResponsible(user)

object Factories {
  val environment = new Environment()
  val idFactory = new PastryIdFactory(environment)
  val bootAddress = "localhost"
  val bootPort = 9000
  val nodeFactory :PastryNodeFactory = new SocketPastryNodeFactory(null, InetAddress.getLocalHost, bootPort, environment);
  val randomNodeIdFactory = new RandomNodeIdFactory(environment)

  def createNode(id: Id) : PastryNode = {

    //val bootSocket = new InetSocketAddress(InetAddress.getByName(bootAddress), bootPort)
    val node = nodeFactory.newNode(id)
    node
  }


}
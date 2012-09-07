import java.net.{InetAddress, InetSocketAddress}
import java.util
import org.jibble.pircbot.User
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
  var channelEvents :Map[String, List[LogEntry]] = Map()

  def merge(other : Log) {
    channelEvents = mergeMap(List(other.channelEvents, channelEvents))(mergeLogMessages(_, _))
  }

  private def mergeLogMessages(a: List[LogEntry], b: List[LogEntry]) : List[LogEntry] = {
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
    addEntry(channel, LogMessage(nick, message))
  }

  def users(channel: String, users: Array[User]) {
    addEntry(channel, LogUserInfo(users.map(_.toString)))
  }

  def joined(channel : String, user: String) {
    addEntry(channel, LogUserJoined(user))
  }

  def action(channel: String, user: String, action: String) {
    addEntry(channel, LogUserJoined(user))
  }

  def notice(channel: String, user: String, notice: String) {
    addEntry(channel, LogNotice(user, notice))
  }

  def part(channel: String, user:String) {
    addEntry(channel, LogPart(user))
  }

  def nickChange(oldNick: String, newNick: String) {
    addGlobalEntry(LogNickChange(oldNick, newNick))
  }

  def kick(channel: String, kickerNick: String, recipientNick:String, reason:String ) {
    addEntry(channel, LogKick(kickerNick, recipientNick, reason))
  }

  def quit(nick: String, reason : String) {
    addGlobalEntry(LogQuit(nick, reason))
  }

  def topic(channel: String, topic: String) {
    addEntry(channel, LogTopic(topic))
  }

  def mode(channel: String, sourceNick:String, mode:String) {
    addEntry(channel, LogMode(sourceNick, mode))
  }

  def invite(sourceNick: String, channel:String) {
    addEntry(sourceNick, LogInvite(sourceNick, channel ))
  }

  def addGlobalEntry(entry: LogEntry) {
    for (channel <- getChannels()) {
      addEntry(channel, entry)
    }
  }

  def addEntry(channel: String, entry: LogEntry) {
    channelEvents += channel -> (entry :: channelEvents.getOrElse(channel, List()))
  }

  def getChannels() : Set[String] = {
    channelEvents.keySet
  }

  def mergeMap[A, B](ms: List[Map[A, B]])(f: (B, B) => B): Map[A, B] = {
    (Map[A, B]() /: (for (m <- ms; kv <- m) yield kv)) {
      (a, kv) =>
        a + (if (a.contains(kv._1)) kv._1 -> f(a(kv._1), kv._2) else kv)
    }
  }
}

abstract class LogEntry() {
  val date = new util.Date()
}
case class LogMessage(nick: String, message:String) extends LogEntry
case class LogUserInfo(users: Array[String]) extends LogEntry
case class LogUserJoined(user: String) extends LogEntry
case class LogAction(user: String, action: String) extends LogEntry
case class LogNotice(user: String, notice: String) extends LogEntry
case class LogPart(user:String) extends LogEntry
case class LogNickChange(oldNick : String, newNick: String) extends LogEntry
case class LogKick(kickerNick: String, recipient: String, reason: String) extends LogEntry
case class LogQuit(user : String, reason: String) extends LogEntry
case class LogTopic(topic: String) extends LogEntry
case class LogMode(user: String, mode: String) extends LogEntry
case class LogInvite(senderNick: String, channel: String) extends LogEntry



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
  var bindPort = 9000
  val bootAddress = new InetSocketAddress(InetAddress.getLocalHost, 9000)

  var nodeFactory :PastryNodeFactory = new SocketPastryNodeFactory(null, InetAddress.getLocalHost, bindPort, environment);
  val randomNodeIdFactory = new RandomNodeIdFactory(environment)

  def createNode(id: Id) : PastryNode = {
    val node = nodeFactory.newNode(id)
    node
  }

  def setBindPort(bindPort : Int) {
    this.bindPort = bindPort
    nodeFactory = new SocketPastryNodeFactory(null, InetAddress.getLocalHost, bindPort, environment);
  }
}
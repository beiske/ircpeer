import java.net.InetAddress
import org.jibble.pircbot.{User, PircBot}
import scala.util.Random

class LoggingIRCClient(user :UserID, channels :Set[String]) extends PircBot with IRCClient {
  val log = new Log()
  setName(user.nick)
  setVerbose(true)


  def start(previousLog : Option[Log]) {
    if (previousLog.isDefined) {
      log.merge(previousLog.get)
    }
    if (!isConnected) {
      val server = IRCClient.getRandomServer(user.network)
      println("Connecting to : " + server + " for " + user)
      connect(server)
      println("Joining channels: " + channels)
      channels.foreach(joinChannel(_))
    }
  }

  def stop() : Log = {
    disconnect()
    dispose()
    log
  }

  def getChannelSet = channels

  def getLog = log

  override def onUserList(channel: String, users: Array[User]) {
    log.users(channel, users)
  }

  override def onJoin(channel :String, sender: String, login: String, hostname:String) {
    if (sender != user.nick) {
      log.joined(channel, sender)
    }
  }

  override def onMessage(channel : String, sender:String, login:String , hostname:String , message:String ) {
    log.message(channel, sender, message)
  }

  override def onPrivateMessage(sender: String, login: String, hostname: String, message: String) {
    log.message(sender, sender, message)
  }

  override def onAction(sender: String, login: String, hostname: String, target: String, action: String) {
    log.action(target, sender, action)
  }

  override def onNotice(sourceNick: String, sourceLogin: String, sourceHostname: String, target: String, notice: String) {
    log.notice(target, sourceNick, notice)
  }

  override def onPart(channel: String, sender: String, login: String, hostname: String) {
    log.part(channel, sender)
  }

  override def onNickChange(oldNick: String, login: String, hostname: String, newNick: String) {
    log.nickChange(oldNick, newNick)
  }

  override def onKick(channel: String, kickerNick: String, kickerLogin: String, kickerHostname: String, recipientNick: String, reason: String) {
    log.kick(channel, kickerNick, recipientNick, reason)
  }

  override def onQuit(sourceNick: String, sourceLogin: String, sourceHostname: String, reason: String) {
    log.quit(sourceNick , reason)
  }

  override def onTopic(channel: String, topic: String) {
    log.topic(channel, topic)
  }

  override def onMode(channel: String, sourceNick: String, sourceLogin: String, sourceHostname: String, mode: String) {
    log.mode(channel, sourceNick, mode)
  }

//  override def onUserMode(targetNick: String, sourceNick: String, sourceLogin: String, sourceHostname: String, mode: String) {
//    println(sourceNick + " set " + mode + " on " + targetNick)
//  }

  override def onInvite(targetNick: String, sourceNick: String, sourceLogin: String, sourceHostname: String, channel: String)  {
    log.invite(sourceNick, channel)
  }

}

trait IRCClient {
  def getLog : Log

  def start(log : Option[Log])

  def stop() : Log

  def getChannelSet : Set[String]
}

object IRCClient {
  val servers : Map[String, List[String]] = Map(
    "EFnet" -> List(
      "irc.homelien.no",
      "efnet.cs.hut.fi",
      //"irc.efnet.no",
      "irc.underworld.no",
      "irc.efnet.nl"),
    "Undernet" -> List(
      "Montreal.QC.CA.Undernet.org",
      "bucharest.ro.eu.undernet.org",
      "Budapest.HU.EU.UnderNet.org",
      "Diemen.NL.EU.Undernet.Org",
      "Lidingo.SE.EU.Undernet.org",
      "Manchester.UK.Eu.UnderNet.org")
  )

  def getRandomServer(network: String):String = {
    val serversForNetwork = servers(network).filter(InetAddress.getByName(_).isReachable(1000))
    val randomIndex = new Random().nextInt(serversForNetwork.length)
    return serversForNetwork(randomIndex)
  }
}
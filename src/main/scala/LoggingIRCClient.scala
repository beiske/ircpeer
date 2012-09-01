import java.net.InetAddress
import org.jibble.pircbot.PircBot
import scala.util.Random

class LoggingIRCClient(user :UserID, channels :Set[String]) extends PircBot with IRCClient {
  val log = new Log()
  setName(user.nick)

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


  override def onMessage (channel: String, sender: String, login: String, hostname: String, message: String) {
    println("channel: " + channel + " sender: " + sender + " login: " + login + " hostname: " + hostname + " message: " + message)
    log.message(channel, sender, message)
  }

  def stop() : Log = {
    disconnect()
    log
  }

  def getChannelSet = channels

  def getLog = log
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
      "irc.efnet.no",
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
    val serversForNetwork = servers(network).filter(InetAddress.getByName(_).isReachable(100))
    val randomIndex = new Random().nextInt(serversForNetwork.length)
    return serversForNetwork(randomIndex)
  }
}
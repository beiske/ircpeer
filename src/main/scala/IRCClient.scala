import java.net.InetAddress
import org.jibble.pircbot.PircBot
import scala.util.Random

class IRCClient(user :UserID, channels :Set[String], log: Log) extends PircBot {
  setName(user.nick)
  val server = IRCClient.getRandomServer(user.network)
  println("Connecting to : " + server + " for " + user)
  connect(server)
  println("Joining channels: " + channels)
  channels.foreach(joinChannel(_))

  override def onMessage (channel: String, sender: String, login: String, hostname: String, message: String) {
    println("channel: " + channel + " sender: " + sender + " login: " + login + " hostname: " + hostname + " message: " + message)
    log.message(channel, sender, message)
  }

  def stop() : Log = {
    disconnect()
    log
  }

  def getLog = log
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
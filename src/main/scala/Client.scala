import org.jibble.pircbot.PircBot
import scala.util.Random

class IRCClient(user :UserID, channels :Set[String]) extends PircBot {
  setName(user.nick)
  connect(IRCClient.getRandomServer(user.network))
  channels.foreach(joinChannel(_))

  override def onMessage (channel: String, sender: String, login: String, hostname: String, message: String) {
    println("channel: " + channel + " sender: " + sender + " login: " + login + " hostname: " + hostname + "message: " + message)
  }
}

object IRCClient extends App {
  val servers = Map(
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
    val serversForNetwork = servers(network)
    val randomIndex = new Random().nextInt(serversForNetwork.length)
    return serversForNetwork(randomIndex)
  }
}
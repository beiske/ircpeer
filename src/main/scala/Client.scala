import org.jibble.pircbot.PircBot

class Client(user : UserID) extends PircBot with IRCClient {
  val log = new Log()
  var channels : Set[String] = Set()
  setName(user.nick)

  val node = Factories.createNode(user.getPastryId());
  val pastry = new PastryActor(node)
  val host = new Host(pastry)
  pastry.registerHost(host)


  override def getLog = log

  override def start(log : Option[Log]) = {
    if (log.isDefined) {
      this.log.merge(log.get)
    }
    //TODO Show history
    println("Got loghistory")
  }

  def parseLine(line : String) {
    line match {
      case join => {

      }
    }
  }

  def stop() : Log = {
    throw new IllegalStateException("Host cannot terminate a user client. Ensure host uses same id as the user client")
  }

  def getChannelSet : Set[String] = channels


}

object Client extends App {
  val join = """^/join (.+)$""".r
}
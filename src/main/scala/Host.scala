import actors.Actor

/**
 * Created by IntelliJ IDEA.
 * User: beiske
 * Date: 23.02.12
 * Time: 16:57
 * To change this template use File | Settings | File Templates.
 */

class Host extends Actor {
    var clients : Map[UserID, IRCClient] = Map()
    override def act() {
      loop {
        react {
          case RequestHostingStart(user, log, channel) => startClient(user, log, channel)
          case RequestHostingEnd(user) => sender ! stopClient(user)
        }
      }
    }

  def startClient(user: UserID, log: Option[Log], channel :Set[String]) {
    val client = new IRCClient(user, channel)
    clients += user -> client
  }

  def stopClient(user :UserID) :Log = {
    new Log()
  }
}
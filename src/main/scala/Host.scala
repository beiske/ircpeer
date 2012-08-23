import actors.{TIMEOUT, Actor}

class Host(pastry : PastryActor) extends Actor {
    var clients : Map[UserID, IRCClient] = Map()
    var watchers : Map[UserID, WatchDog] = Map()
    override def act() {
      loop {
        react {
          case RequestHostingStart(user, log, channel) => startClient(user, log, channel)
          case RequestHostingEnd(user) => sender ! stopClient(user)
          case HeartBeat(logs) => for ((user, log) <- logs) {
            if (!watchers.isDefinedAt(user)) {
              val watcher = new WatchDog(user, pastry);
              watcher.start()
              watchers += user -> watcher
            }
            watchers(user) ! log
          }
          case TIMEOUT => {
            pastry ! HeartBeat(clients.mapValues((client : IRCClient) => {
              client.getLog
            }))
            new SingleEventTimer(Host.milliSecondsBetweenHeartBeats, this).start()
          }
        }
      }
    }

  def startClient(user: UserID, log: Option[Log], channel :Set[String]) {
    val mergedLog = log.getOrElse(new Log())
    if (clients.contains(user)) {
      //TODO don't restart for duplicate request
      mergedLog.merge(clients(user).stop())
    }
    val client = new IRCClient(user, channel, mergedLog)
    clients += user -> client

  }

  def stopClient(user :UserID) :Log = {
    clients(user)
    new Log()
  }
}

object Host {
  val milliSecondsBetweenHeartBeats = 3000
}

class WatchDog(user : UserID, pastry: PastryActor) extends Actor {
  var log : Option[Log] = None
  def act {
    loop {
      reactWithin(Host.milliSecondsBetweenHeartBeats*2) {
        case TIMEOUT => pastry ! RequestHostingStart(user, log, Set())
        case logMessage:Log => log = Some(logMessage)
      }
    }
  }
}


class SingleEventTimer(delay: Int, target : Actor) extends Actor {
  def act {
    reactWithin(delay) {
      case TIMEOUT => target ! TIMEOUT
    }
  }
}
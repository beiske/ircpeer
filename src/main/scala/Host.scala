import actors.{TIMEOUT, Actor}

class Host(pastry : PastryActor) extends Actor {
    var clients : Map[UserID, IRCClient] = Map()
    var watchers : Map[UserID, WatchDog] = Map()
    override def act() {
      loop {
        react {
          case RequestHostingStart(user, log, channel) => startClient(user, log, channel)
          case RequestHostingEnd(user) => sender ! stopClient(user)
          case HeartBeat(logs, channels) =>
            for ((user, channelSet) <- channels) {
              if (!watchers.isDefinedAt(user) || watchers(user).getState == Actor.State.Terminated) {
                val watcher = new WatchDog(user, pastry);
                watcher.start()
                watchers += user -> watcher
              }
              watchers(user) ! (logs(user), channelSet)
            }
          case TIMEOUT => {
            pastry ! HeartBeat(clients.mapValues((client : IRCClient) => {
              client.getLog
            }).map(identity), clients.mapValues((client : IRCClient) => {
              client.getChannelSet
            }).map(identity))
            new SingleEventTimer(Host.milliSecondsBetweenHeartBeats, this).start()
          }
          case RequestTransfer(senderId) => {
            val clientsToTransfer = clients.filterKeys(_.getPastryId().isBetween(senderId, pastry.endpoint.getId))
            clientsToTransfer.foreach( (tuple : Tuple2[UserID, IRCClient]) => {
            sender ! RequestHostingStart(tuple._1, Some(stopClient(tuple._1)), tuple._2.getChannelSet)

          })
          }
        }
      }
    }

  def startClient(user: UserID, log: Option[Log], channel :Set[String]) {
    val client = if (clients.contains(user)) {
      clients(user)
      //TODO handle updated channel set
    } else {
      val client = new LoggingIRCClient(user, channel)
      clients += user -> client
      client
    }
    client.start(log)


  }

  def stopClient(user :UserID) :Log = {
    val client = clients(user)
    val log = client.stop()
    clients = clients - user
    log
  }
}

object Host {
  val milliSecondsBetweenHeartBeats = 3000
}

class WatchDog(user : UserID, pastry: PastryActor) extends Actor {
  var log : Option[Log] = None
  var channels : Set[String] = null
  def act {
    loop {
      reactWithin(Host.milliSecondsBetweenHeartBeats*10) {
        case TIMEOUT => {
          println("Timeout, requesting hosting for: " + user + " " + channels)
          pastry ! RequestHostingStart(user, log, channels)
          exit()
        }
        case (logMessage:Log, channelSet:Set[String] )=>{
          log = Some(logMessage)
          channels = channelSet
        }
        case x => println("Got:" + x)
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
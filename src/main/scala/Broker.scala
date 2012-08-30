import actors.{TIMEOUT, Actor}
import actors.Actor._
import collection.mutable
import rice.p2p.commonapi._

/**
 * This class bridges Pastry communication and
 */
class PastryActor(node: Node) extends Application with Actor {
  var host: Host = null
  val endpoint = node.buildEndpoint(this, "ircpeer")
  //val backupNodes: mutable.PriorityQueue[NodeHandle] = new mutable.PriorityQueue()

  implicit def toOrdered(a: NodeHandle): Ordered[NodeHandle] = new Ordered[NodeHandle] {
    override def compare(b: NodeHandle): Int = {
      a.getId.distanceFromId(node.getId).compareTo(b.getId.distanceFromId(node.getId))
    }
  }

  override def update(nodeHandle: NodeHandle, joined: Boolean) {
    if (joined) {
      new NodeHandleActorAdaptor(nodeHandle, host, RequestTransfer(nodeHandle.getId.asInstanceOf[rice.pastry.Id]), endpoint).start()

//      backupNodes += nodeHandle
    } //else {
    // backupNodes -= nodeHandle
    //}
  }

  override def deliver(id: Id, message: Message) {
    message match {
      case Envelope(sender, contents) => new NodeHandleActorAdaptor(sender, host, contents, endpoint).start()
      case h : HeartBeat => host ! h
      case m: Message => println("Received message without envelope: " + m.toString)
    }
  }

  override def forward(routeMessage: RouteMessage) = true

  override def act() {
    loop {
      react {
        case m: MessageToUserResponsible => endpoint.route(m.user.getPastryId, new Envelope(node.getLocalNodeHandle, m), null)
        case m: HeartBeat => {
          val target = getHeartBeatRecipient()
          if (target.isDefined)
            endpoint.route(null, m, target.get)
        }
      }
    }

  }



  def getHeartBeatRecipient() : Option[NodeHandle] = {
    val neighbours = endpoint.neighborSet(4)
    if (neighbours.size > 1) {
      Some((0 until neighbours.size()).map(neighbours.getHandle(_)).filter(_.getId.clockwise(node.getId)).min)
    } else if (neighbours.size == 1) {
      Some(neighbours.getHandle(0))
    } else {
      None
    }

  }

  def registerHost(host: Host) {
    this.host = host
    endpoint.register()
    host.start()
    start()
    host ! TIMEOUT
  }
}

class NodeHandleActorAdaptor(sender: NodeHandle, target: Actor, message: Any, endpoint: Endpoint) extends Actor {
  target ! message

  override def act {
    loop {
      react {
        case m: Message => endpoint.route(null, m, sender)
      }
    }
  }
}

case class Envelope(sender: NodeHandle, message: Message) extends Message {
  override def getPriority: Int = {
    message.getPriority
  }

}
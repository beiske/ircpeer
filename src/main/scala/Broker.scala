import actors.{TIMEOUT, Actor}
import actors.Actor._
import collection.mutable
import rice.p2p.commonapi._
import rice.pastry.PastryNode

/**
 * This class bridges Pastry communication and
 */
class PastryActor(node: PastryNode) extends Application with Actor {
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
      val adaptor = new NodeHandleActorAdaptor(nodeHandle, endpoint)
      adaptor.start()
      adaptor ! ActorEnvelope(host, RequestTransfer(nodeHandle.getId.asInstanceOf[rice.pastry.Id]))
//      backupNodes += nodeHandle
    } else {
    // backupNodes -= nodeHandle
      println("Node left: " + nodeHandle)
    }
  }

  override def deliver(id: Id, message: Message) {
    message match {
      case Envelope(sender, contents) => {
        val adaptor = new NodeHandleActorAdaptor(sender, endpoint)
        adaptor.start()
        adaptor ! ActorEnvelope(host, contents)
      }
      case h : HeartBeat => host ! h
      case m : HostingStarted => host ! m
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
      val beforeAndAfter = (0 until neighbours.size()).map(neighbours.getHandle(_)).partition(_.getId.clockwise(node.getId))
      if (beforeAndAfter._1.length > 0) {
        Some(beforeAndAfter._1.min)
      } else {
        Some(beforeAndAfter._2.max)
      }
    } else if (neighbours.size == 1) {
      Some(neighbours.getHandle(0))
    } else {
      None
    }

  }

  def registerHost(host: Host) {
    this.host = host
    host.start()
    start()
    endpoint.register()
    node.boot(Factories.bootAddress)
    host ! TIMEOUT
  }
}

class NodeHandleActorAdaptor(sender: NodeHandle, endpoint: Endpoint) extends Actor {

  override def act {
    loop {
      react {
        case m: Message => {
          println("Routing reply: " + m + " to " + sender)
          endpoint.route(null, m, sender)
        }
        case ActorEnvelope(target, message) => target ! message
        case x => println("Adapter got: " + x)
      }
    }
  }
}

case class ActorEnvelope(target:Actor, message:Any)

case class Envelope(sender: NodeHandle, message: Message) extends Message {
  override def getPriority: Int = {
    message.getPriority
  }

}
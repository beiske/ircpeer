import actors.Actor
import actors.Actor._
import rice.p2p.commonapi._

/**
 * Created by IntelliJ IDEA.
 * User: beiske
 * Date: 22.02.12
 * Time: 21:21
 * To change this template use File | Settings | File Templates.
 */
class PastryActor(node :Node, ircClient :Actor) extends Application with Actor {
  val endpoint = node.buildEndpoint(this, "ircpeer")


  override def update(nodeHandle : NodeHandle, joined: Boolean) {

  }

  override def deliver(id :Id, message: Message) {
    message match {
      case Envelope(sender, contents) => new NodeHandleActorAdaptor(sender, ircClient, contents, endpoint).start()
      case m : Message => println("Received message without envelope: " + m.toString)
    }
  }

  override def forward(routeMessage : RouteMessage) = true

  override def act() {
    loop {
      react {
        case m : MessageToUserResponsible => endpoint.route(m.user.getPastryId, new Envelope(node.getLocalNodeHandle, m), null)
      }
    }

  }
  endpoint.register()
}

class NodeHandleActorAdaptor(sender: NodeHandle, target: Actor, message :Any, endpoint: Endpoint) extends Actor {
  target ! message

  override def act {
    loop {
      react {
        case m : Message => endpoint.route(null, m, sender)
      }
    }
  }
}

case class Envelope(sender:NodeHandle, message :Message) extends Message {
  override def getPriority :Int = {
    message.getPriority
  }

}
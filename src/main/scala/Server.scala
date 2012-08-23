/**
 * Created with IntelliJ IDEA.
 * User: beiske
 * Date: 13.08.12
 * Time: 21:48
 * To change this template use File | Settings | File Templates.
 */

class Server {

  val node = Factories.createNode(Factories.randomNodeIdFactory.generateNodeId());
  val pastry = new PastryActor(node)
  val host = new Host(pastry)
  pastry.registerHost(host)

}

object Server extends App {
  new Server()
  new Server().host ! RequestHostingStart(UserID("omnidux", "EFnet"), None, Set("#ircpeer", "#java.no"))

}
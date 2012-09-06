class Server {

  val node = Factories.createNode(Factories.randomNodeIdFactory.generateNodeId());
  val pastry = new PastryActor(node)
  val host = new Host(pastry)
  pastry.registerHost(host)

}

object Server extends App {
  new Server()
  new Server().pastry ! RequestHostingStart(UserID("omnidux", "EFnet"), None, Set("#ircpeer", "#java.no"))

}
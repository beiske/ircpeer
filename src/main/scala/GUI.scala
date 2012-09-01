import java.net.{InetAddress, InetSocketAddress}



class Controller {
  var listenPort = 0

  var user: UserID = null
  var pastry : PastryActor = null
  var host : Host = null


  def config() {
    //TODO use actual config dialog
    listenPort = 9001

    user = UserID("omnidux", "EFnet")


  }

  def connectToPastry() {

    val node = Factories.createNode(user.getPastryId())
    pastry = new PastryActor(node)
    host = new Host(pastry)
    pastry.registerHost(host)
  }

}

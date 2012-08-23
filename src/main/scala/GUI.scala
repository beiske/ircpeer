import java.net.{InetAddress, InetSocketAddress}

/**
 * Created with IntelliJ IDEA.
 * User: beiske
 * Date: 13.08.12
 * Time: 21:07
 * To change this template use File | Settings | File Templates.
 */

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

import java.net.InetSocketAddress

/**
 * Created with IntelliJ IDEA.
 * User: beiske
 * Date: 15.07.12
 * Time: 16:08
 * To change this template use File | Settings | File Templates.
 */

class Scenario1 extends App {
  val bootAddress = new InetSocketAddress("localhost", 9000);
  for (val i <- 0 until 5) {
    //new Main(i, bootAddress)
  }
  println("Clients started")

}

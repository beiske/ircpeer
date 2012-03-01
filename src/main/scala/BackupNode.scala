import java.io.IOException
import java.net.{InetAddress, InetSocketAddress}
import rice.environment.Environment
import rice.pastry.socket.SocketPastryNodeFactory
import rice.pastry.standard.RandomNodeIdFactory
import rice.pastry.{PastryNode, PastryNodeFactory, NodeIdFactory}

/**
 * Created by IntelliJ IDEA.
 * User: beiske
 * Date: 15.01.12
 * Time: 15:44
 * To change this template use File | Settings | File Templates.
 */


class BackupNode(bindPort: Int, bootAdress: InetSocketAddress, env: Environment) {

  // Generate the NodeIds Randomly
  val nidFactory :NodeIdFactory = new RandomNodeIdFactory(env);

  // construct the PastryNodeFactory, this is how we use rice.pastry.socket
  val factory :PastryNodeFactory = new SocketPastryNodeFactory(nidFactory, bootAdress.getAddress, bindPort, env);

  // construct a node, but this does not cause it to boot
  val node = factory.newNode();

  // in later tutorials, we will register applications before calling boot
  node.boot(bootAdress);

  // the node may require sending several messages to fully boot into the ring
  node synchronized {
    while (!node.isReady() && !node.joinFailed()) {
      // delay so we don't busy-wait
      node.wait(500);

      // abort if can't join
      //if (node.joinFailed()) {
      //  throw new IOException("Could not join the FreePastry ring.  Reason:" + node.joinFailedReason());
      //}
    }
  }

  System.out.println("Finished creating new node " + node);
}


object BackupNode extends App {
  // Loads pastry settings
  val env = new Environment();

  // disable the UPnP setting (in case you are testing this on a NATted LAN)
  env.getParameters.setString("nat_search_policy", "never");

  try {
    // the port to use locally
    val bindPort = Integer.parseInt(args(0));

    // build the bootaddress from the command line args
    val host = InetAddress.getByName(args(1));
    val port = Integer.parseInt(args(2));
    val bootAddress = new InetSocketAddress(host, port);

    // launch our node!
    val dt = new BackupNode(bindPort, bootAddress, env);
  } catch {
    case e: Exception => {
      // remind user how to use
      System.out.println("Usage:");
      System.out.println("java [-cp FreePastry-<version>.jar] rice.tutorial.lesson1.DistTutorial localbindport bootIP bootPort");
      System.out.println("example java rice.tutorial.DistTutorial 9001 pokey.cs.almamater.edu 9001");
      throw e;
    }
  }
}
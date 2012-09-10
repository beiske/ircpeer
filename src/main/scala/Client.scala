import actors.Actor
import collection.JavaConverters.asJavaCollectionConverter
import java.io.InputStreamReader
import java.net.{InetAddress, InetSocketAddress}
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.util.regex.Pattern
import org.jibble.pircbot.{User, PircBot}
import util.matching.Regex

class Client(user : UserID) extends PircBot with IRCClient {

  setName(user.nick)
  setVerbose(false)

  val node = Factories.createNode(user.getPastryId());
  val pastry = new PastryActor(node)
  val host = new Host(pastry)
  host.clients += user -> this
  pastry.registerHost(host)

  var currentChannel :String = null

  val shell = new Shell(new InputHandler(this))


  val server = IRCClient.getRandomServer(user.network)

  println("Connecting to : " + server + " for " + user)
  connect(server)
  println("Connected")


  override def getLog = new Log()

  def println(line : String) {
    shell.println(line)
  }

  override def start(logOption : Option[Log]) = {
    for (log <- logOption) {
      println("Got loghistory")
      for ((channel, events)<-log.channelEvents) {
        for (event <- events.reverse) {
          println(event.toString(channel))
        }
      }
    }



  }

  def selectChannel(channel: String) {
    if (!getChannels.contains(channel)) {
      joinChannel(channel)
    } else if (channel != currentChannel) {
      currentChannel = channel
      shell.setPrompt(channel)
    }
  }

  override def onChannelInfo(channel: String, users: Int, topic: String) {
    println("Found channel: " + channel + " users: " + users + " topic: " + topic)
  }

  override def onUserList(channel: String, users: Array[User]) {
    val result = new StringBuilder(channel + ": ")
    users.addString(result, ", ")
    println(result.toString)
  }

  override def onJoin(channel :String, sender: String, login: String, hostname:String) {
    if (sender == user.nick) {
      currentChannel = channel
      shell.setPrompt(channel)
    }
    println(sender + " joined " + channel)
  }

  override def onMessage(channel : String, sender:String, login:String , hostname:String , message:String ) {
    println(channel + " : " + sender + "> " + message)
  }

  override def onPrivateMessage(sender: String, login: String, hostname: String, message: String) {
    println(sender + ">" + message)
  }

  override def onAction(sender: String, login: String, hostname: String, target: String, action: String) {
    println(target + " : * " + sender + " " + action)
  }

  override def onNotice(sourceNick: String, sourceLogin: String, sourceHostname: String, target: String, notice: String) {
    println(target + " : " + sourceNick + " > " + notice)
  }

  override def onPart(channel: String, sender: String, login: String, hostname: String) {
    println(sender + " left " + channel)
  }

  override def onNickChange(oldNick: String, login: String, hostname: String, newNick: String) {
    println(oldNick + " is now know as " + newNick)
  }

  override def onKick(channel: String, kickerNick: String, kickerLogin: String, kickerHostname: String, recipientNick: String, reason: String) {
    println(channel + ": " + recipientNick + " was kicked by " + kickerLogin + " becuase: " + reason)
  }

  override def onQuit(sourceNick: String, sourceLogin: String, sourceHostname: String, reason: String) {
    println(sourceNick + " quit: " + reason)
  }

  override def onTopic(channel: String, topic: String) {
    println(channel + " now has topic: " + topic)
  }

  override def onMode(channel: String, sourceNick: String, sourceLogin: String, sourceHostname: String, mode: String) {
    println(channel + ": " + sourceNick + " set mode " + mode)
  }

  override def onUserMode(targetNick: String, sourceNick: String, sourceLogin: String, sourceHostname: String, mode: String) {
    println(sourceNick + " set " + mode + " on " + targetNick)
  }

  override def onInvite(targetNick: String, sourceNick: String, sourceLogin: String, sourceHostname: String, channel: String)  {
    println(sourceNick + " invites you to join " + channel)
  }



  def stop() : Log = {
    throw new IllegalStateException("Host cannot terminate a user client. Ensure host uses same id as the user client")
  }

  def getChannelSet : Set[String] = Set(getChannels :_*)

}

class InputHandler(client: Client) extends Actor {
  val opHandler = new SecondaryActionHandler(client)

  override def act() {
    loop {
      react {
        case line : String => parseLine(line)
      }
    }
  }

  def parseLine(line: String) {
    if (line.startsWith("/")) {
      //client.println("About to match command: " + line)
      line match {
        case Client.join(channel) => {
          client.selectChannel(channel)
        }
        case Client.part(channel) => {
          client.partChannel(channel)
        }
        case Client.message(target, message) => {
          client.sendMessage(target, message)
        }
        case Client.me(action) => {
          client.sendAction(client.currentChannel, action)
        }
        case Client.notice(target, notice) => {
          client.sendNotice(target, notice)
        }
        case Client.mode(channel, mode) => {
          client.setMode(channel, mode)
        }
        case Client.invite(nick, channel) => {
          client.sendInvite(nick, channel)
        }
        case Client.topic(channel, topic) => {
          client.setTopic(channel, topic)
        }
        case _ => opHandler.opActions(line)
      }
    } else if (client.currentChannel != null) {
      client.sendMessage(client.currentChannel, line)
    }
  }

  /**
   * This class is a workaround of the large class size generated by the scala compiler when having several cases in one match statement
   * @param client
   */
  class SecondaryActionHandler(client: Client) {
    def opActions(line : String) {
      line match {
        case Client.listAllChannels => {
          client.listChannels()
        }
        case Client.listUsers(channel) => {
          client.onUserList(channel, client.getUsers(channel))
        }
        case Client.ban(channel, hostMask) => {
          client.ban(channel, hostMask)
        }
        case Client.unban(channel, hostMask) => {
          client.unBan(channel, hostMask)
        }
        case Client.op(channel, nick) => {
          client.op(channel, nick)
        }
        case Client.deop(channel, nick) => {
          client.deOp(channel, nick)
        }
        case Client.voice(channel, nick) => {
          client.voice(channel, nick)
        }
        case Client.devoice(channel, nick) => {
          client.deVoice(channel, nick)
        }
        case Client.kick(channel, nick) => {
          client.kick(channel, nick)
        }
        case line : String => client.println("Unknown command: " + line)
      }
    }

  }
  start()
}


/**
 * Usage: scala Client <username> <network>
 *    or: scala Client <username> <network> <localPort>
 */
object Client extends App {
  private def createSingleArgPattern(command : String) : Regex = {
    ("^/"+command+" (.+)$").r
  }

  private def createTwoArgPattern(command : String) : Regex = {
    ("^/"+command+" (\\w+) (.+)$").r
  }

  val join = createSingleArgPattern("join")
  val part = createSingleArgPattern("part")
  val message = createTwoArgPattern("privmessage")
  val notice = createTwoArgPattern("notice")
  val mode = createTwoArgPattern("mode")
  val invite = createTwoArgPattern("invite")
  val ban = createTwoArgPattern("ban")
  val unban = createTwoArgPattern("unban")
  val op = createTwoArgPattern("op")
  val deop = createTwoArgPattern("deop")
  val voice = createTwoArgPattern("voice")
  val devoice = createTwoArgPattern("devoice")
  val topic = createTwoArgPattern("topic")
  val kick = createTwoArgPattern("kick")
  val listAllChannels = """^/listAllChannels$""".r
  val listUsers = createSingleArgPattern("listUsers")
  val changeChannel = createSingleArgPattern("changeChannel")
  val me = createSingleArgPattern("me")

  if (args.length >= 5) {
    Factories.bootAddress = new InetSocketAddress(InetAddress.getByName(args(3), Integer.valueOf(args(4))))
  }
  if (args.length >= 3) {
    Factories.setBindPort(Integer.valueOf(args(2)))
  }
  if (args.length >= 2) {
    new Client(new UserID(args(0), args(1)))
  } else {
    println("Usage: scala Client <username> <network>")
  }
}

/**
 * A simple shell implementation that handles reading input while printing output
 *
 * TODO find a better third party replacement
 * @param inputHandler
 */
class Shell(inputHandler : Actor) {
  var prompt = ">"
  var buffer = ""
  def setPrompt(prompt : String) {
    this.prompt = prompt + ">"
    print("\n" + prompt)
  }

  def println(line : String) {
    print("\r" + line + (" " * scala.math.max(prompt.size + buffer.size + 2 - line.size, 0))+ "\n" + prompt + buffer)
  }

  new Thread(new Runnable {
    def run() {
      val input = new InputStreamReader(System.in)
      val newLine = '\n'.toInt
      val backSpace = 127
      while (true) {
        val nextChar = input.read()
        if (nextChar == -1 || nextChar == newLine) {
          inputHandler ! buffer
          buffer = ""
          print(prompt)
        } else if (nextChar == backSpace && buffer.size > 0) {
          buffer = buffer.substring(0, buffer.size -1)
          print("\r"+ prompt + buffer)
        } else {
          buffer += nextChar.toChar
        }
      }
    }
  }).start()
}
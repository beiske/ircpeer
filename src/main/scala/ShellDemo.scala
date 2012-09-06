import actors.Actor

object ShellDemo extends App {


  val a = new Actor() {
    override def act() {
      loop {
        react {
          case "exit\n" => System.exit(0) //Does not work..
          case x: String => System.err.print("Hello: " + x)
        }
      }
    }
  }
  a.start()

  val s = new Shell(a)
  s.setPrompt("What is your name?")



  new Thread(new Runnable() {
    def run() {
      var count = 0
      while (true) {
        s.println("Count is: " + count)
        count += 1
        Thread.sleep(1000)
      }
    }
  }).start()
}

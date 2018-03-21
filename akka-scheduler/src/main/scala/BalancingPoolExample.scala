import akka.actor._
import akka.routing._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.Future
import com.typesafe.config.ConfigFactory

object BalancingPoolExample extends App with Logging {
  val config = ConfigFactory.load()

  val runTime = Runtime.getRuntime()
  println("Number of processors " + runTime.availableProcessors)

  val system = ActorSystem("FunSystem")
  implicit val context = system.dispatchers.lookup("my-dispatcher")

  logStatus("start")

  // Refer to https://doc.akka.io/docs/akka/2.5.4/scala/routing.html#balancing-pool
  // Refert to Akka test routing.BlancingSpec
  val master = system.actorOf(Props[Master], name = "master")
  master ! START


  //master ! END


}

class Master extends Actor with Logging {
  def receive = {
    case START =>
      logStatus("Master " + self.path.name)
      val router: ActorRef =
        context.actorOf(BalancingPool(3).props(Props[Worker]), "router")

      (1 to 3) map {i =>
        router ! i
      }

    case END     =>
      println("Master end")
      context.stop(self)

    case x =>
      logStatus(s"Master received $x")
  }

}

class Worker extends Actor with Logging {
  def receive = {
    case i: Int =>
      logStatus("Worker " + self.path.name + s" received $i")
      sender() ! (i + 10) // This message goes back to Master actor
      //if (i < 10)
      //  self ! (i + 10)
      //Thread.sleep(1000)
    case x =>
      // Should never come here
      logStatus("Worker " + self.path.name + s" received $x")
      context.stop(self)
  }
}

trait Logging {
  def logStatus(msg: String): Unit = {
    val timestamp: Long = System.currentTimeMillis / 1000
    val threadId = Thread.currentThread().getId()
    println(s"$msg Current time $timestamp and thread ID $threadId ")
  }

}

case object START
case object END




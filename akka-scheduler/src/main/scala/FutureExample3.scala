import akka.actor.ActorSystem
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.Future
import com.typesafe.config.ConfigFactory

object FutureExample3 extends App {
  val config = ConfigFactory.load()

  val runTime = Runtime.getRuntime()
  println("Number of processors " + runTime.availableProcessors)

  val system = ActorSystem("FunSystem")
  implicit val executionContext = system.dispatchers.lookup("my-dispatcher")

  logStatus("start")


  doStepOne onComplete { x =>
    logStatus(s"$x-end")
  }


  def doStepOne = Future {
    logStatus("one")
    (1 to 5) map {
      i => doStep(i)
    }
  }

  def doStep(i: Int) = Future {
    logStatus(s"$i")
    sleepNow(5)
    i
  }

  def logStatus(step: String): Unit = {
    val timestamp: Long = System.currentTimeMillis / 1000
    val threadId = Thread.currentThread().getId()
    println(s"Step $step Current time $timestamp and thread ID $threadId ")
  }

  def sleepNow(sleepTime: Long) = {
    Thread.sleep(sleepTime * 1000) // 100 seconds
  }

}


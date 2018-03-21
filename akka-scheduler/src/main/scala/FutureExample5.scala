import akka.actor.ActorSystem
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.Future
import com.typesafe.config.ConfigFactory

object FutureExample5 extends App {
  val config = ConfigFactory.load()

  val runTime = Runtime.getRuntime()
  println("Number of processors " + runTime.availableProcessors)

  val system = ActorSystem("FunSystem")
  implicit val executionContext = system.dispatchers.lookup("my-dispatcher")

  logStatus("start")


  //https://doc.akka.io/docs/akka/2.5/futures.html#auxiliary-methods
  val result = doStepOne fallbackTo doFallback1() fallbackTo doFallback2()
  result foreach println

  def doStepOne = Future {
    logStatus("one")
    "one"
    throw new Exception("one failed")
  }

  def doFallback1() = Future {
    logStatus(s"Fall back 1")
    "fallback1"
  }

  def doFallback2() = Future {
    logStatus(s"Fall back 2")
    "fallback2"
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


import akka.actor.ActorSystem
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.Future
import com.typesafe.config.ConfigFactory
import scala.util.control.NonFatal

object FutureRecoverExample extends App {
  val config = ConfigFactory.load()

  val runTime = Runtime.getRuntime()
  println("Number of processors " + runTime.availableProcessors)

  val system = ActorSystem("FunSystem")
  implicit val executionContext = system.dispatchers.lookup("my-dispatcher")

  logStatus("start")

  doStepOne map { x =>
    logStatus(s"$x-end")
    throw new Exception("step exception")
  } recover {
    case NonFatal(e) =>
      println("Exception " + e.getMessage)
  }

  def doStepOne = Future {
    logStatus("one")
    sleepNow(3)
    1
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


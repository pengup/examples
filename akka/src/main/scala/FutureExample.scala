import akka.actor.ActorSystem
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.Future
import com.typesafe.config.ConfigFactory

object FutureExample extends App {
  val config = ConfigFactory.load()

  val runTime = Runtime.getRuntime()
  println("Number of processors " + runTime.availableProcessors)

  val system = ActorSystem("FunSystem")
  implicit val executionContext = system.dispatchers.lookup("my-dispatcher")
  //implicit val executionContext = system.dispatchers.d
  // efaultGlobalDispatcher
  //println(config.getObject("akka.actor.default-dispatcher"))

  logStatus("start")

  doStepOne map { x =>
    logStatus(s"$x-end")
    doStepTwo map { y =>
      logStatus(s"$y-end")
      doStepThree map {
        z => logStatus(s"$z-end")
      }
    }
  }

  doStepThree

  def doStepOne = Future {
    logStatus("one")
    sleepNow(3)
    1
  }

  def doStepTwo = Future {
    logStatus("two")
    sleepNow(5)
    2
  }

  def doStepThree = Future {
    logStatus("three")
    sleepNow(2)
    logStatus("three-end")
    3
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


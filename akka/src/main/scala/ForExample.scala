import akka.actor.ActorSystem
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.Future
import com.typesafe.config.ConfigFactory

object ForExample extends App {
  val config = ConfigFactory.load()

  val runTime = Runtime.getRuntime()
  println("Number of processors " + runTime.availableProcessors)

  val system = ActorSystem("FunSystem")
  implicit val executionContext = system.dispatchers.lookup("my-dispatcher")


  val result = for (
    i <- doStepOne;
    j <- doStepTwo;
    k <- doStepThree
  ) yield (i, j, k)

  // TIP: This print 1,2,3 in sequence order because For comprehension is actually using flatMap
  result map {
    i => println(s"$i " + i._1 + i._2)
  }

  println("end")

  def doStepOne: Future[Int] = Future {
    sleepNow(1)
    logStatus("one")
    1
  }

  def doStepTwo: Future[Int] = Future {
    sleepNow(5)
    logStatus("two")
    2
  }

  def doStepThree: Future[Int] = Future {
    sleepNow(2)
    logStatus("three")
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


import akka.actor.ActorSystem
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.Future
import com.typesafe.config.ConfigFactory
import akka.pattern.after

object AkkaPatternExample extends App {
  val config = ConfigFactory.load()

  val runTime = Runtime.getRuntime()
  println("Number of processors " + runTime.availableProcessors)

  val system = ActorSystem("FunSystem")
  implicit val executionContext = system.dispatchers.lookup("my-dispatcher")

  logStatus("start")

  //https://doc.akka.io/docs/akka/2.5/futures.html#after
  val delayed = akka.pattern.after(200 millis, using = system.scheduler)(Future.successful(
    "delayed"))

  val future = Future {
    Thread.sleep(2000)
    "foo" }

  val output = Future firstCompletedOf Seq(future, delayed)
  output foreach println

  def logStatus(step: String): Unit = {
    val timestamp: Long = System.currentTimeMillis / 1000
    val threadId = Thread.currentThread().getId()
    println(s"Step $step Current time $timestamp and thread ID $threadId ")
  }

}


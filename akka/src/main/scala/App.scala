import akka.actor.ActorSystem
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.Future
import com.typesafe.config.ConfigFactory

object Main extends App {
  val config = ConfigFactory.load()

  val runTime = Runtime.getRuntime()
  println("Number of processors " + runTime.availableProcessors)

  val system = ActorSystem("FunSystem")
  implicit val executionContext = system.dispatchers.lookup("my-dispatcher")
  //implicit val executionContext = system.dispatchers.defaultGlobalDispatcher
  //println(config.getObject("akka.actor.default-dispatcher"))

  system.scheduler.schedule(
    1 second,
    1 seconds
  )(
    doSomething
  )

  def doSomething = Future {
    val timestamp: Long = System.currentTimeMillis / 1000
    val threadId = Thread.currentThread().getId()
    println(s"Current time $timestamp and thread ID $threadId ")
    Thread.sleep(100000) // 100 seconds
  }

}


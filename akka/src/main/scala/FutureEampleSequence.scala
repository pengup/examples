import akka.actor.ActorSystem
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.Future
import com.typesafe.config.ConfigFactory
import scala.util.control.NonFatal

object FutureExampleSequence extends App {
  val config = ConfigFactory.load()

  val runTime = Runtime.getRuntime()
  println("Number of processors " + runTime.availableProcessors)

  val system = ActorSystem("FunSystem")
  implicit val executionContext = system.dispatchers.lookup("my-dispatcher")
  //implicit val executionContext = system.dispatchers.d
  // efaultGlobalDispatcher
  //println(config.getObject("akka.actor.default-dispatcher"))

  logStatus("start")

  val listOfFutures = Seq(
    Future { true },
    Future { throw new RuntimeException("e1") }.recover { case NonFatal(e) => println(e); false },
    Future { throw new RuntimeException("e2") }.recover { case NonFatal(e) => println(e); false },
    Future { true }
  )

  // This will return
  // List(true, false, false, true)
  Future.sequence(listOfFutures).map(println)

  val listOfFutures2 = Seq(
    Future { true },
    Future { throw new RuntimeException("e1") }.recoverWith { case NonFatal(e) => println(e); Future.failed(e) },
    Future { throw new RuntimeException("e2") }.recoverWith { case NonFatal(e) => println(e); Future.failed(e) },
    Future { true }
  )

  // This will return
  // "Seq failed java.lang.RuntimeException: e1"
  Future.sequence(listOfFutures2).map(println).recover {case e => println("Seq failed " + e) }

  def logStatus(step: String): Unit = {
    val timestamp: Long = System.currentTimeMillis / 1000
    val threadId = Thread.currentThread().getId()
    println(s"Step $step Current time $timestamp and thread ID $threadId ")
  }

  def sleepNow(sleepTime: Long) = {
    Thread.sleep(sleepTime * 1000) // 100 seconds
  }

}


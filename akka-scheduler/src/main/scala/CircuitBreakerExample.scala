import akka.actor.{ ActorSystem, Actor, ActorLogging, ActorRef }
import akka.pattern.CircuitBreaker
import akka.pattern.pipe
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.duration._

import scala.concurrent.duration._
import scala.concurrent.Future
import com.typesafe.config.ConfigFactory

import scala.concurrent.Future
import scala.util.{ Failure, Success, Try }

object CircuitBreakerExample extends App {
  val config = ConfigFactory.load()

  val runTime = Runtime.getRuntime()
  println("Number of processors " + runTime.availableProcessors)

  test()

  def doFuture(sleepMills: Long) = Future {
    Thread.sleep(sleepMills)
    100
  }

  def printResult(value: Try[Int]): Unit = {
    val timestamp: Long = System.currentTimeMillis / 1000
    val threadId = Thread.currentThread().getId()
    println(s"Current time $timestamp and thread ID $threadId and result $value ")
  }

  def notifyMeOnOpen(): Unit =
    println("My CircuitBreaker is now open, and will not close for a while")


  def test()={
    val system = ActorSystem("PingPongSystem")
    val breaker =
      new CircuitBreaker(
        system.scheduler,
        maxFailures = 2,
        callTimeout = 1.seconds,
        resetTimeout = 5 seconds).onOpen(notifyMeOnOpen())


    breaker.withCircuitBreaker(doFuture(10)).onComplete((value: Try[Int]) => printResult(value) )
    breaker.withCircuitBreaker(doFuture(1500)).onComplete((value: Try[Int]) => printResult(value) )
    breaker.withCircuitBreaker(doFuture(1500)).onComplete((value: Try[Int]) => printResult(value) )
    Thread.sleep(5000)
    breaker.withCircuitBreaker(doFuture(10)).onComplete((value: Try[Int]) => printResult(value) )
    breaker.withCircuitBreaker(doFuture(10)).onComplete((value: Try[Int]) => printResult(value) )
    Thread.sleep(7000)
    println("continue")
    breaker.withCircuitBreaker(doFuture(10)).onComplete((value: Try[Int]) => printResult(value) )
    breaker.withCircuitBreaker(doFuture(10)).onComplete((value: Try[Int]) => printResult(value) )
    breaker.withCircuitBreaker(doFuture(10)).onComplete((value: Try[Int]) => printResult(value) )

  }

}

class DangerousActor extends Actor with ActorLogging {

  import context.dispatcher

  val breaker =
    new CircuitBreaker(
      context.system.scheduler,
      maxFailures = 5,
      callTimeout = 10.seconds,
      resetTimeout = 1.minute).onOpen(notifyMeOnOpen())

  def notifyMeOnOpen(): Unit =
    log.warning("My CircuitBreaker is now open, and will not close for one minute")

  def dangerousCall: String = "This really isn't that dangerous of a call after all"

  def receive = {
    case "is my middle name" ⇒
      breaker.withCircuitBreaker(Future(dangerousCall)) pipeTo sender()
    case "block for me" ⇒
      sender() ! breaker.withSyncCircuitBreaker(dangerousCall)
  }

  def luckyNumber(): Future[Int] = {
    val evenNumberAsFailure: Try[Int] ⇒ Boolean = {
      case Success(n) ⇒ n % 2 == 0
      case Failure(_) ⇒ true
    }

    val breaker =
      new CircuitBreaker(
        context.system.scheduler,
        maxFailures = 5,
        callTimeout = 10.seconds,
        resetTimeout = 1.minute)

    // this call will return 8888 and increase failure count at the same time
    breaker.withCircuitBreaker(Future(8888), evenNumberAsFailure)
  }


}
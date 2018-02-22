import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorSystem, Props}
import akka.pattern._
import akka.util.Timeout
import tasks._

import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, ExecutionContext, Future}

/**
  *
  * The code example is from [here](https://github.com/shekhargulati/52-technologies-in-2016/tree/master/41-akka-dispatcher)
  *
  */

class TaskExecutionEngine(system: ActorSystem) {

  def run(tasks: List[Task]): List[Status] = {
    implicit val executionContext: ExecutionContext = system.dispatchers.defaultGlobalDispatcher
    val futures = tasks.map(task => {
      val actorRef = system.actorOf(Props[TaskActor])
      implicit val timeout: Timeout = Timeout(1, TimeUnit.MINUTES)
      (actorRef ? task).map(_.asInstanceOf[Status])
    })
    val statuses: List[Status] = Await.result(Future.sequence(futures), Duration.Inf)
    statuses
  }

}

object TaskExecutionEngine {
  def apply(): TaskExecutionEngine = new TaskExecutionEngine(ActorSystem("tasky"))
}

class TaskActor extends Actor {
  override def receive: Receive = {
    case task: Task =>
      val result = task()
      sender() ! result
  }
}


object tasks {

  trait Status

  case object success extends Status

  case class Failure(code: Int, message: String) extends Status

  type Task = () => Status

  class WaitTask(timeToWait: Duration = 5 seconds) extends Task {
    override def apply(): Status = {
      println(s"${LocalDateTime.now()} >> Executing WaitTask...")
      Thread.sleep(timeToWait.toMillis)
      println(s"${LocalDateTime.now()} >> Executed WaitTask...")
      success
    }
  }

  class CmdTask(cmd: String) extends Task {

    import sys.process._

    override def apply(): Status = {
      println(s"${LocalDateTime.now()} >> Executing CmdTask...")
      val exitCode = cmd.!
      println(s"${LocalDateTime.now()} >> Executed CmdTask...")
      if (exitCode == 0) success else Failure(exitCode, s"command $cmd failed")
    }
  }
}

import scala.concurrent.duration._

object Tasky extends App {
  val engine = TaskExecutionEngine()

  val statuses = engine.run(List(new WaitTask(5 seconds), new WaitTask(5 seconds), new WaitTask(5 seconds)))
  statuses.foreach(println(_))
  //engine.shutdown()
}
import java.nio.file.Paths

import akka.NotUsed
import akka.actor.{Actor, ActorSystem, Cancellable, Props}
import akka.event.Logging
import akka.stream._
import akka.stream.scaladsl.Tcp.ServerBinding
import akka.stream.scaladsl.{Flow, _}
import akka.util.ByteString
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Random

object StreamExample extends App {
  val config = ConfigFactory.load()

  val runTime = Runtime.getRuntime()
  println("Number of processors " + runTime.availableProcessors)

  implicit val system = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()

  def testOne = {
    val source: Source[Int, NotUsed] = Source(1 to 3)
    source.runForeach(i ⇒ println(i))(materializer)

    val factorials = source.scan(BigInt(1))((acc, next) ⇒ acc * next)

    val result: Future[IOResult] =
      factorials
        .map(num ⇒ ByteString(s"$num\n"))
        .runWith(FileIO.toPath(Paths.get("factorials.txt")))

    def lineSink(filename: String): Sink[String, Future[IOResult]] =
      Flow[String]
        .map(s ⇒ ByteString(s + "\n"))
        .toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)

    factorials.map(_.toString).runWith(lineSink("factorial2.txt"))
  }

  def testSourceTick = {
    val tick: Source[Int, Cancellable] = Source.tick(0.seconds, 5.seconds, 1)
    tick.runForeach(println)
  }

  def testSourceRepeat = {
    val tick: Source[String, NotUsed] = Source.repeat("test")
    tick.runForeach(println)
  }

  def testBackPressure = {
    def slowComputation(i:Int) = {
      Thread.sleep(1000)
      i
    }
    val source: Source[Int, NotUsed] = Source(1 to 10)
    source.buffer(1, OverflowStrategy.dropHead)
      .map(slowComputation)
      .runForeach(println)

  }

  def testFuture = {
    def doWork: Future[Int] = Future {
      Thread.sleep(1000)
      100
    }
    Source.fromFuture(doWork)
      .runForeach(x => println("Future " + x))
  }


  def testFuture2 = {
    val source = Source(1 to 10)

    // Using Future will not ensure the order then
    def output(i: Int) = Future {
      Thread.sleep(1000)
      println(i)
    }

    source.runForeach(output)
  }

  // Materialize by Source runWith
  def testRunWith = {
    val source = Source(1 to 10)
    val sink = Sink.fold[Int, Int](0)(_ + _)

    // materialize the flow, getting the Sinks materialized value
    val sum: Future[Int] = source.runWith(sink)
    println(sum)
  }


  def testGraph = {
    // connect the Source to the Sink, obtaining a RunnableGraph
    val sink = Sink.fold[Int, Int](0)(_ + _)
    val runnable: RunnableGraph[Future[Int]] =
      Source(1 to 10).toMat(sink)(Keep.right)

    // get the materialized value of the FoldSink
    val sum1: Future[Int] = runnable.run()
    val sum2: Future[Int] = runnable.run()

    // sum1 and sum2 are different Futures!
  }

  def testSourceToSink = {
    // Explicitly creating and wiring up a Source, Sink and Flow
    val graph: RunnableGraph[NotUsed] = Source(1 to 6).via(Flow[Int].map(_ * 2)).to(Sink.foreach(println(_)))

    // Starting from a Source
    val source = Source(1 to 6).map(_ * 2)
    val graph1: RunnableGraph[NotUsed] = source.to(Sink.foreach(println(_)))

    // Starting from a Sink
    val sink: Sink[Int, NotUsed] = Flow[Int].map(_ * 2).to(Sink.foreach(println(_)))
    Source(1 to 6).to(sink)

    // Broadcast to a sink inline
    val otherSink: Sink[Int, NotUsed] =
      Flow[Int].alsoTo(Sink.foreach(println(_))).to(Sink.ignore)
    Source(1 to 6).to(otherSink)
  }

  def testMapAsync = {
    val system = ActorSystem("FunSystem")
    val executionContext = system.dispatchers.lookup("blocking-dispatcher")

    val source = Source(1 to 10)

    // Use a separate execution context for a future
    def wait(i: Int) =  Future {
      Thread.sleep(1000)
      i
    }(executionContext)

    // mapAsync
    val next = source.mapAsync(3)(i => wait(i))

    val sink: RunnableGraph[NotUsed] =
      next.map(println).to(Sink.ignore)

    sink.run()
  }


  def testOverflowNotWorking = {
    val source = Source(1 to 10)

    // This will never overflow because source is created by stream, which has back pressure in place
    source.buffer(1, OverflowStrategy.dropNew)

    def wait(i: Int): Int =  {
      Thread.sleep(1000)
      i
    }

    val process: Flow[Int, Int, NotUsed] =  Flow[Int].map(wait)

    val sink = Flow[Int].to(Sink.foreach(println))

    source.via(process).to(sink).run()

  }

  def testOverflow = {
    def wait(i: Int) =  {
      Thread.sleep(1000)
      println(i)
    }

    val source = Source.actorRef[Int](1, OverflowStrategy.dropNew)

    // ref is only avaialbe after run, i.e., materialized
    val ref = Flow[Int].to(Sink.foreach(wait)).runWith(source)

    (1 to 10) map {i =>
      ref ! i
    }

  }

  def testActor = {
    def wait(i: Int) =  {
      Thread.sleep(1000)
      println(i)
    }

    val source = Source(1 to 10)
    source.buffer(1, OverflowStrategy.dropNew)

    import akka.util.Timeout

    import scala.concurrent.duration._

    implicit val duration: Timeout = 20 seconds

    class DestActor extends Actor {
      def receive = {
        case i: Int =>
          sender() ! i

      }
    }

    val dstRef = system.actorOf(Props(new DestActor), name = "dst")

    source.ask[Int](parallelism = 5)(dstRef)
      // continue processing of the replies from the actor
      .runWith(Sink.foreach(wait))

  }

  def testLog = {
    Source(-5 to 5)
      .map(1 / _) //throwing ArithmeticException: / by zero
      .log("error logging")
      .runWith(Sink.ignore)

  }

  def testLog2 = {
    Source(-5 to 5)
      .map(1 / _) //throwing ArithmeticException: / by zero
      .log("logging")
      .withAttributes(
        Attributes.logLevels(
          onElement = Logging.InfoLevel,
          onFinish = Logging.InfoLevel,
          onFailure = Logging.ErrorLevel // Logging.InfoLevel here will not log the exception? This seems a bug
        )
      ).runWith(Sink.ignore)
  }

  def testLogging = {
    val source = Source(1 to 3)
    source.map{ i =>
      println(i)
      i
    }.runWith(Sink.ignore)

  }

  def testRecover = {
    Source(0 to 6).map(n ⇒
      if (n < 5) n.toString
      else throw new RuntimeException("Boom!")
    ).recover {
      case _: RuntimeException ⇒ "stream truncated"
    }.runForeach(println)
  }

  def testRecoverWithRetries = {
    val planB = Source(List("five", "six", "seven", "eight"))

    Source(0 to 6).map(n ⇒
      if (n < 5) n.toString
      else throw new RuntimeException("Boom!")
    ).recoverWithRetries(attempts = 1, {
      case _: RuntimeException ⇒ planB
    }).runForeach(println)
  }

  def testBind = {
    val binding: Future[ServerBinding] =
      Tcp().bind("127.0.0.1", 8888).to(Sink.foreach(println)).run()

  }

  def testStreamOrder = {
    val source = Source(1 to 3)

    logThread("start")
    // The stream is run in an actor with a different thread
    source.map(i => {
        logThread("stream map: " + i)
        i
      }
    ).runForeach(i => println("stream run " + i))

    // This may be printed before logs in stream because Stream is run in a different actor
    Thread.sleep(Random.nextInt(100))
    logThread("end")
  }

  def logThread(step: String): Unit = {
    val timestamp: Long = System.currentTimeMillis / 1000
    val threadId = Thread.currentThread().getId()
    println(s"Step $step Current time $timestamp and thread ID $threadId ")
  }

  testStreamOrder


}


import java.nio.file.Paths

import akka.NotUsed
import akka.actor.{ActorSystem, Cancellable}
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString
import com.typesafe.config.ConfigFactory

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

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


  testSourceToSink

}


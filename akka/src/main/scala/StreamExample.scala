import java.nio.file.Paths

import akka.NotUsed
import akka.actor.{ActorSystem, Cancellable}
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString
import com.typesafe.config.ConfigFactory

import scala.concurrent.Future
import scala.concurrent.duration._

object StreamExample extends App {
  val config = ConfigFactory.load()

  val runTime = Runtime.getRuntime()
  println("Number of processors " + runTime.availableProcessors)

  implicit val system = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()

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

  val tick: Source[Int, Cancellable] = Source.tick(0.seconds, 1.seconds, 1)
  source.runForeach(println)


}


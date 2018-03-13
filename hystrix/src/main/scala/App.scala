import scala.concurrent.duration._
import HystrixDSL._


object Main extends App {
  implicit val config = HystrixConfig("Hystrix-test", "web", Some(5 second))

  val urls = List("http://www.google.com:81",
     "http://127.0.0.1:8080/")
  gracefully {
    println(urls(1) + " content length:" + get(urls(1)).length)
    println(urls(0) + " content length:" + get(urls(0)).length)
  } fallback {
    println("fallback")
  }

  @throws(classOf[java.io.IOException])
  def get(url: String) = io.Source.fromURL(url).mkString

}



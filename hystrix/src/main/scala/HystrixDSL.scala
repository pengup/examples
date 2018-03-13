import com.netflix.hystrix.HystrixCommand
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global

object HystrixDSL {
  def gracefully[T](command: => T)(implicit config: HystrixConfig) = new Object {
    /** Gracefully execute one code block and fallback to another code block upon encountering an error */
    def fallback(fallback: => T): T = new GracefulFallback[T](command, fallback, config).execute()

    /** Gracefully degrade by failing fast to enable rapid recovery */
    def degrade(): T = new GracefulDegradation[T](command, config).execute()
  }

  def async[T](command: => T)(implicit config: HystrixConfig): Future[T] = {
    def action(): T = new GracefulDegradation[T](command, config).execute()
    Future[T](action)
  }
}


private class GracefulFallback[T](command: => T, fallback: => T, config: HystrixConfig)
  extends HystrixCommand[T](Hystrix.CommandSetter(config)) {
  def run(): T = command

  override def getFallback: T = fallback
}

private class GracefulDegradation[T](command: => T, config: HystrixConfig)
  extends HystrixCommand[T](Hystrix.CommandSetter(config)) {
  def run(): T = command
}
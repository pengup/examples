import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy
import scala.concurrent.duration.Duration

case class HystrixConfig(group: String,
                         command: String,
                         timeout: Option[Duration] = None,
                         isolation: Option[ExecutionIsolationStrategy] = None,
                         isolationSemaphoreMax: Option[Int] = None,
                         threadPoolSize: Option[Int] = None)
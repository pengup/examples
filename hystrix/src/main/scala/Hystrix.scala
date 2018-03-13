import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisher
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy
import com.netflix.hystrix.strategy.executionhook.HystrixCommandExecutionHook
import com.netflix.hystrix._
import com.netflix.hystrix.strategy.HystrixPlugins

object Hystrix {
  private val monitor = new Object
  private var factory: Option[SetterFactory] = None

  def init(eventNotifier: Option[HystrixEventNotifier] = None,
           concurrencyStrategy: Option[HystrixConcurrencyStrategy] = None,
           metricsPublisher: Option[HystrixMetricsPublisher] = None,
           propertiesFactory: Option[HystrixPropertiesStrategy] = None,
           commandExecutionHook: Option[HystrixCommandExecutionHook] = None): SetterFactory = {
    monitor.synchronized {
      if (factory.isEmpty) {
        val pluginRegistry = HystrixPlugins.getInstance()

        for (notifier <- eventNotifier) pluginRegistry.registerEventNotifier(notifier)
        for (concurrency <- concurrencyStrategy) pluginRegistry.registerConcurrencyStrategy(concurrency)
        for (publisher <- metricsPublisher) pluginRegistry.registerMetricsPublisher(publisher)
        for (properties <- propertiesFactory) pluginRegistry.registerPropertiesStrategy(properties)
        for (hook <- commandExecutionHook) pluginRegistry.registerCommandExecutionHook(hook)

        factory = Some(new SetterFactory)
      }
    }
    factory.get
  }

  def CommandSetter(config: HystrixConfig): HystrixCommand.Setter = factory.getOrElse(init()).create(config)
}

class SetterFactory {
  def create(config: HystrixConfig): HystrixCommand.Setter = {
    val setter = HystrixCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(config.group))
      .andCommandKey(HystrixCommandKey.Factory.asKey(config.command))
      .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(config.command))

    val commandProperties = HystrixCommandProperties.Setter()
    for (timeout <- config.timeout) {
      commandProperties.withExecutionIsolationThreadTimeoutInMilliseconds(timeout.toMillis.toInt)
    }
    for (isolationStrategy <- config.isolation) {
      commandProperties.withExecutionIsolationStrategy(isolationStrategy)
    }
    for (semaphoreMax <- config.isolationSemaphoreMax) {
      commandProperties.withExecutionIsolationSemaphoreMaxConcurrentRequests(semaphoreMax)
    }
    setter.andCommandPropertiesDefaults(commandProperties)

    val threadPoolProperties = HystrixThreadPoolProperties.Setter()
    for (poolSize <- config.threadPoolSize) {
      threadPoolProperties.withCoreSize(poolSize)
    }
    setter.andThreadPoolPropertiesDefaults(threadPoolProperties)

    setter
  }
}
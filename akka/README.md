# Akka scheduler
The code examines how `system.scheduler.schedule` schedules a `Future` function.

Note that is not recommended to put Future inside an actor.

For AKKA dispatcher, refer to [AKKA dispatcher](https://github.com/pengup/52-technologies-in-2016/tree/master/41-akka-dispatcher)

Run an example, e.g.,

```bash
sbt "runMain FutureExample"
```
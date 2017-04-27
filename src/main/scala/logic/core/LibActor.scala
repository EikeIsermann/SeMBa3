package logic.core

import akka.actor.{Actor, ActorRef}
import logic.core.jobHandling.{JobHandling, SingleJobExecutor}
import logic.core.jobHandling.benchmark.Benchmarking

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
trait LibActor extends JobHandling with Benchmarking {
    val config: Config
    override var benchmarkActor: Option[ActorRef] = config.benchmarkActor 
}

trait LibJobExecutor extends SingleJobExecutor {
  val  config: Config
  override var benchmarkActor: Option[ActorRef] = config.benchmarkActor

}

trait GlobalJobExecutor extends SingleJobExecutor {
  override var benchmarkActor: Option[ActorRef] = None

}
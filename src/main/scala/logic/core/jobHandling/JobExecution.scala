package logic.core.jobHandling

import akka.actor.{Actor, ActorRef}
import logic.core.{ActorFeatures, Config}
import logic.core.jobHandling.benchmark.Benchmarking

/**
  * Author: Eike Isermann
  * This is a Default (Template) Project class
  */
trait JobExecution extends Actor with ActorFeatures {

  override def receive: Receive = {
    case job: Job => {
      handleJob(job, sender())
    }
    case x => super.receive(x)
  }

  def handleJob(job: Job, master: ActorRef)

  def finishedJob(job: Job, master: ActorRef, results: ResultArray)

  def postAcceptHook(job: Job, master: ActorRef): Unit = {}

  def postFinishHook(job: Job, results: ResultArray): Unit = {}
}

trait SingleJobExecutor extends JobExecution with Benchmarking {

  override def handleJob(job: Job, master: ActorRef) = {
    postAcceptHook(job, master)
    val result = performTask(job)
    finishedJob(job, context.sender(), new ResultArray(result))
  }

  def performTask(job:Job): JobResult

  override def finishedJob(job: Job, master: ActorRef, results: ResultArray): Unit = {
    postFinishHook(job, results)
    context.sender() ! JobReply(job, results)
  }
}


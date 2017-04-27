package logic.core.jobHandling

import akka.actor.{Actor, ActorRef}
import logic.core.{ActorFeatures, LibActor}

/**
  * Author: Eike Isermann
  * This is a Default (Template) Project class
  */
trait JobExecution extends Actor with ActorFeatures {

  override def receive: Receive = {
    case job: Job => handleJob(job, sender())
    case x => super.receive(x)
  }

  def handleJob(job: Job, master: ActorRef)

  def finishedJob(job: Job, master: ActorRef, results: ResultArray)

  def finishOperations(job: Job, results: ResultArray): Unit = {}
}

trait SingleJobExecutor extends JobExecution {

  override def handleJob(job: Job, master: ActorRef) = {
    val result = performTask(job)
    finishedJob(job, context.sender(), new ResultArray(result))
  }

  def performTask(job:Job): JobResult

  override def finishedJob(job: Job, master: ActorRef, results: ResultArray): Unit = {
    finishOperations(job, results)
    context.sender() ! JobReply(job, results)
  }
}

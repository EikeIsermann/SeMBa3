package data.storage

import akka.actor.{Actor, ActorRef, Props}
import akka.actor.Actor.Receive
import logic.core.{JobHandling, JobProtocol, JobReply, JobResult}

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
class ReadExecutor(storage: SembaStorageComponent) extends Actor with JobHandling{
  override def receive: Receive = ???

  override def finishedJob(job: JobProtocol, master: ActorRef, results: ResultArray[JobResult]): Unit = ???
}

object ReadExecutor {
  def props(storage: SembaStorageComponent): Props = Props(new ReadExecutor(storage))
}

package logic.core

import akka.actor.{Actor, ActorRef, Props}
import globalConstants.GlobalMessages.{StorageReadRequest, StorageWriteRequest}

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
class StorageQueryExecutor(storage: ActorRef) extends Actor with JobHandling {
  override def receive: Receive = {
    case read: StorageReadRequest => {

    }

    case write: StorageWriteRequest => {

      {}
    }

    case other => super.receive(other)
  }

  override def finishedJob(job: JobProtocol, master: ActorRef, results: ResultArray[JobResult]): Unit = ???
}

object StorageQueryExecutor {

  def props(storage: ActorRef): Props = Props(new StorageQueryExecutor(storage))

}

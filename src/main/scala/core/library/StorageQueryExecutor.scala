package core.library

import akka.actor.{Actor, ActorRef, Props}
import akka.actor.Actor.Receive
import core.library.StorageQueryExecutor.{StorageReadRequest, StorageWriteRequest}
import core.{JobHandling, JobProtocol, JobReply}

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
  override def handleJob(jobProtocol: JobProtocol): JobReply = ???

}

object StorageQueryExecutor {

  def props(storage: ActorRef): Props = Props(new StorageQueryExecutor(storage))


  trait StorageOperation extends JobProtocol
  case class StorageReadRequest() extends StorageOperation
  case class StorageWriteRequest() extends StorageOperation

}

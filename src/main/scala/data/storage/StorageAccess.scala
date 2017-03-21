package data.storage

import akka.actor.{Actor, ActorRef, Props}
import akka.actor.Actor.Receive
import core.{JobHandling, JobProtocol, JobReply}

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
class StorageAccess() extends Actor with JobHandling {
  override def receive: Receive = ???

  override def handleJob(jobProtocol: JobProtocol): JobReply = ???
}

object StorageAccess{
  def props(): Props = Props(new StorageAccess)

}

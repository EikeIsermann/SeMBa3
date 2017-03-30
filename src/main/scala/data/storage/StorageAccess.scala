package data.storage

import akka.actor.{Actor, ActorRef, Props}
import akka.actor.Actor.Receive
import logic.core._

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
class StorageAccess() extends Actor with ActorFeatures with JobHandling {
   def wrappedReceive: Receive = ???

  override def finishedJob(job: JobProtocol, master: ActorRef, results: ResultArray): Unit = ???
}

object StorageAccess{
  def props(): Props = Props(new StorageAccess)

}

package data.storage

import akka.actor.Actor
import akka.actor.Actor.Receive
import core.{JobHandling, JobProtocol, JobReply}

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
class ReadExecutor extends Actor with JobHandling{
  override def receive: Receive = ???

  override def handleJob(jobProtocol: JobProtocol): JobReply = ???
}

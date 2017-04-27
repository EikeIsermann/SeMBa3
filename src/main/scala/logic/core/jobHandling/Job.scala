package logic.core.jobHandling

import java.lang.System.{nanoTime ⇒ timestamp}
import java.util.UUID

import akka.actor.ActorRef
import globalConstants.GlobalMessages.StorageWriteResult
import logic.core._
import sembaGRPC.UpdateMessage

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
//trait JobResult
trait Job {
  var jobID: UUID = UUID.randomUUID()
  var parentCall = jobID
  def newId() = jobID = UUID.randomUUID()
}

case class ErrorResult(payload: String = "Error in Job") extends ResultContent
case class JobReply(job: Job, result: ResultArray)

case class EmptyResult(payload: Any = None) extends ResultContent()








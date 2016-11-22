package core

import java.io.File
import java.net.URI
import java.util.UUID

import akka.actor.ActorRef
import sembaGRPC.UpdateMessage

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
trait JobProtocol {
  var jobID: UUID = UUID.randomUUID()
}

case class ThumbnailJob(src: File, dest: URI, config: Config) extends JobProtocol

case class MainJob() extends JobProtocol

case class JobReply(job: JobProtocol, updates: ArrayBuffer[UpdateMessage] = ArrayBuffer[UpdateMessage]())

trait JobHandling {
  var waitingForCompletion = Set.empty[(JobProtocol, JobProtocol)]
  var originalSender =  mutable.HashMap[UUID, ActorRef]()
  val updates =  mutable.HashMap[UUID, ArrayBuffer[UpdateMessage]]()
  def handleJob(jobProtocol: JobProtocol): JobReply

  def handleReply(reply: JobReply, selfRef: ActorRef): Boolean = {
    var completed = false
    val entry = waitingForCompletion.find(_._1.jobID == reply.job.jobID)
    if (entry.isDefined) {

      updates.apply(entry.get._2.jobID).++=(reply.updates)
      waitingForCompletion -= entry.get
      completed = !waitingForCompletion.exists(_._2.jobID == entry.get._2.jobID)
      if (completed) {
        println("Job finished:" + entry.get._1.getClass + this.getClass)
        val jobMaster = originalSender.apply(entry.get._2.jobID)
        if (jobMaster != selfRef ) {
          jobMaster ! JobReply(entry.get._2, updates.apply(entry.get._2.jobID))
          updates.remove(entry.get._2.jobID)
        }
        else processUpdates(entry.get._2)
      }
    }

    completed

  }

  def processUpdates(jobProtocol: JobProtocol) = {
      updates.remove(jobProtocol.jobID)
  }

  def acceptJob(newJob: JobProtocol, sender: ActorRef): Unit = {
    createMasterJob(newJob, sender)
  }


  def createJob(newJob: JobProtocol, originalJob: JobProtocol): JobProtocol = {
    waitingForCompletion.+=((newJob, originalJob))
    newJob
  }

  def createMasterJob(newJob: JobProtocol, actorRef: ActorRef): JobProtocol = {
    updates.put(newJob.jobID, ArrayBuffer[UpdateMessage]())
    originalSender.put(newJob.jobID, actorRef)
    waitingForCompletion.+=((newJob, newJob))
    newJob
  }


}

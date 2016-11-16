package core

import java.io.File
import java.net.URI
import java.util.UUID

import akka.actor.ActorRef

import scala.collection.mutable

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
trait JobProtocol {
  var jobID: UUID = UUID.randomUUID()
}

case class ThumbnailJob(src: File, dest: URI, config: Config) extends JobProtocol

case class MainJob() extends JobProtocol

case class JobReply(job: JobProtocol)

trait JobHandling {
  var waitingForCompletion = Set.empty[(JobProtocol, JobProtocol)]
  var originalSender = new mutable.HashMap[JobProtocol, ActorRef]()

  def handleJob(jobProtocol: JobProtocol): JobReply

  def handleReply(reply: JobReply): Boolean = {
    var completed = false
    val entry = waitingForCompletion.find(_._1.jobID == reply.job.jobID)
    if (entry.isDefined) {

      waitingForCompletion -= entry.get
      completed = !waitingForCompletion.exists(_._2.jobID == entry.get._2.jobID)
      if (completed) {
        println("Job finished:" + entry.get)
        originalSender.apply(entry.get._2) ! JobReply(entry.get._2)
      }
    }

    completed

  }

  def acceptJob(newJob: JobProtocol, sender: ActorRef): Unit = {
    createMasterJob(newJob, sender)

  }

  def createJob(newJob: JobProtocol, originalJob: JobProtocol): JobProtocol = {
    waitingForCompletion.+=((newJob, originalJob))
    newJob
  }

  def createMasterJob(newJob: JobProtocol, actorRef: ActorRef): JobProtocol = {
    originalSender.put(newJob, actorRef)
    waitingForCompletion.+=((newJob, newJob))
    newJob
  }


}

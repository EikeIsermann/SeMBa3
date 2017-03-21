package core

import java.io.File
import java.net.URI
import java.util.UUID

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorRef}
import sembaGRPC.UpdateMessage

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
trait JobProtocol {
  var jobID: UUID = UUID.randomUUID()
  def newId() = jobID = UUID.randomUUID()
}
case class JobResult()

case class JobReply(job: JobProtocol, result: JobResult)

trait JobHandling extends Actor {
  var waitingForCompletion = Set.empty[(UUID, JobProtocol)]
  var originalSender =  mutable.HashMap[UUID, ActorRef]()
  val results =  mutable.HashMap[UUID, ArrayBuffer[JobResult]]()
  //def handleJob(T <: JobProtocol[T])

  def handleReply(reply: JobReply): Boolean = {
    var completed = false
    val entry = waitingForCompletion.find(_._1 == reply.job.jobID)
    if (entry.isDefined) {

      results.apply(entry.get._2.jobID).+=(reply.result)
      waitingForCompletion -= entry.get
      completed = !waitingForCompletion.exists(_._2.jobID == entry.get._2.jobID)
      if (completed) {
        val jobMaster = originalSender.apply(entry.get._2.jobID)
        val originalJob = entry.get._2
        val resultBuffer = results.apply(originalJob.jobID)

        finishedJob(originalJob, jobMaster, resultBuffer)
        results.remove(entry.get._2.jobID)
        originalSender.remove(entry.get._2.jobID)
      }
    }
    completed

  }

  def finishedJob(job: JobProtocol, master: ActorRef, results: ArrayBuffer[JobResult])

  def acceptJob(newJob: JobProtocol, sender: ActorRef): JobProtocol = {
    results.put(newJob.jobID, ArrayBuffer[JobResult]())
    originalSender.put(newJob.jobID, sender)
    newJob
  }


  def createJob(newJob: JobProtocol, originalJob: JobProtocol): JobProtocol = {
    waitingForCompletion.+=((newJob.jobID, originalJob))
    newJob
  }

  def createJobCluster(cluster: JobProtocol, originalJob: JobProtocol): JobProtocol = {
     createJob(cluster, originalJob)
     createMasterJob(cluster)
  }

  def createMasterJob(newJob: JobProtocol): JobProtocol = {
    acceptJob(newJob, self)
  }

  def sendJobReply(job: JobProtocol, master: ActorRef, result: JobResult) = {
    master ! JobReply(job, result)
  }

}

trait JobExecution extends Actor{

  override def receive: Receive = {
    case job: JobProtocol => {
      sender() ! JobReply(job, handleJob(job))
    }
    case other => super.receive(other)
  }

  def handleJob(job: JobProtocol): JobResult
}

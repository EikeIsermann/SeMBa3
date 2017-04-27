package logic.core.jobHandling

import java.util.UUID

import akka.actor.{Actor, ActorRef}
import logic.core.ActorFeatures

import scala.collection.mutable

/**
  * Author: Eike Isermann
  * This is a Default (Template) Project class
  */
trait JobHandling extends ActorFeatures with JobExecution {

  var waitingForCompletion = Set.empty[(UUID, Job)]
  var originalSender =  mutable.HashMap[UUID, ActorRef]()
  val jobResults =  mutable.HashMap[UUID, ResultArray]()
  //def handleJob(T <: JobProtocol[T])

  override def receive: Receive = {
    case reply: JobReply => handleReply(reply)
    case x => super.receive(x)
  }

  def handleReply(reply: JobReply): Boolean = {
    var completed = false
    val entry = waitingForCompletion.find(_._1 == reply.job.jobID)
    if (entry.isDefined) {
      val originalJob = entry.get._2
      val newResults = reply.result
      jobResults.apply(originalJob.jobID).add(newResults.results)
      reactOnReply(reply, originalJob, jobResults.apply(originalJob.jobID))
      waitingForCompletion -= entry.get
      completed = !waitingForCompletion.exists(_._2.jobID == entry.get._2.jobID)
      if (completed) {
        val jobMaster = originalSender.apply(entry.get._2.jobID)
        val resultBuffer = jobResults.apply(originalJob.jobID)
        finishOperations(originalJob, resultBuffer)
        finishedJob(originalJob, jobMaster, resultBuffer)
        jobResults.remove(originalJob.jobID)
        originalSender.remove(originalJob.jobID)
      }
    }
    completed

  }
  def reactOnReply(reply: JobReply, originalJob: Job, results: ResultArray) = {

  }

  def handleJob(job: Job, master: ActorRef)

  def finishedJob(job: Job, master: ActorRef, results: ResultArray): Unit = {
    master ! JobReply(job, results)
  }

  def forwardJob(job: Job, sender: ActorRef): Job = {
    acceptJob(job, sender)
    createJob(job,job)
  }


  def acceptJob(newJob: Job, sender: ActorRef): Job = {
    jobResults.put(newJob.jobID, new ResultArray)
    originalSender.put(newJob.jobID, sender)
    //executionTime.put(newJob.jobID, System.currentTimeMillis())
    newJob
  }


  def createJob(newJob: Job, originalJob: Job): Job = {
    waitingForCompletion.+=((newJob.jobID, originalJob))
    newJob.parentCall = originalJob.parentCall
    newJob
  }

  def createJobCluster(cluster: Job, originalJob: Job): Job = {
     createJob(cluster, originalJob)
     createMasterJob(cluster)
  }

  def createMasterJob(newJob: Job): Job = {

    acceptJob(newJob, self)
    newJob
  }

  def sendJobReply(job: Job, master: ActorRef, result: ResultArray) = {
    master ! JobReply(job, result)
  }


}

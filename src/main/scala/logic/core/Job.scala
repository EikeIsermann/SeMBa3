package logic.core

import java.util.UUID

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorRef}
import globalConstants.GlobalMessages.{StorageWriteResult}
import sembaGRPC.UpdateMessage
import utilities.debug.DC

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
//trait JobResult
case class JobResult(val content: ResultContent)
trait ResultContent{val payload: Any}
case class ErrorResult(payload: String = "Error in Job") extends ResultContent
case class JobReply(job: JobProtocol, result: ResultArray)
case class EmptyResult(val payload: Any = None) extends ResultContent()

trait JobHandling extends Actor with ActorFeatures {

  var waitingForCompletion = Set.empty[(UUID, JobProtocol)]
  var originalSender =  mutable.HashMap[UUID, ActorRef]()
  val jobResults =  mutable.HashMap[UUID, ResultArray]()
  //def handleJob(T <: JobProtocol[T])

  override def receive: Receive = {
    case reply: JobReply => {
      DC.log("JobReply received by: " + self + "  Reply: " + reply.job.toString )
      handleReply(reply)
    }
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

        finishedJob(originalJob, jobMaster, resultBuffer)
        jobResults.remove(entry.get._2.jobID)
        originalSender.remove(entry.get._2.jobID)
      }
    }
    completed

  }
  def reactOnReply(reply: JobReply, originalJob: JobProtocol, results: ResultArray) = {

  }


  def finishedJob(job: JobProtocol, master: ActorRef, results: ResultArray): Unit ={
    DC.log("Finished Job " + job)
    master ! JobReply(job, results)
  }

  def forwardJob(job: JobProtocol, sender: ActorRef): JobProtocol = {
    acceptJob(job, sender)
    createJob(job,job)
  }


  def acceptJob(newJob: JobProtocol, sender: ActorRef): JobProtocol = {
    jobResults.put(newJob.jobID, new ResultArray)
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

  def sendJobReply(job: JobProtocol, master: ActorRef, result: ResultArray) = {
    master ! JobReply(job, result)
  }


}

class ResultArray(input: ArrayBuffer[JobResult] = ArrayBuffer[JobResult]())
{
  def this(single: JobResult) = {
    this(ArrayBuffer[JobResult](single))
  }

  var results = input
  def get[T <: ResultContent]( aClass: Class[T]): T  = {
    results.find(x => x.content.getClass.equals(aClass)).get.content.asInstanceOf[T]
  }
  def getAll[T <: ResultContent]( aClass: Class[T]): ArrayBuffer[T] = {
    results.filter( x => x.content.getClass.equals(aClass)).map( x => x.content.asInstanceOf[T])
  }

  def processUpdates(): ArrayBuffer[UpdateMessage] = {
    getAll(classOf[StorageWriteResult]).map(x => x.payload)
  }

  def extract[T]( aClass: Class[T]): T  = {
    results.find(x => x.content.payload.getClass.equals(aClass)).get.content.payload.asInstanceOf[T]
    //this.find(x => x.content.
    //x.content.content.getClass.equals(aClass)).get.content.asInstanceOf[T]
  }
  def add(toAdd: ArrayBuffer[JobResult]) =
  {
    results = results ++ toAdd
  }

}



trait JobExecution extends Actor {
  override def receive: Receive = {
    case job: JobProtocol => {
      sender() ! JobReply(job, new ResultArray(handleJob(job)))
    }
  }

  def handleJob(job: JobProtocol): JobResult
}

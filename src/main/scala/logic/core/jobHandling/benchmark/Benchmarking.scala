package logic.core.jobHandling.benchmark

import java.util.UUID

import akka.actor.ActorRef
import System.{nanoTime ⇒ time}

import api.SembaApiCall
import logic.core.jobHandling.{Job, JobExecution, JobHandling, ResultArray}

import scala.collection.mutable

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
trait Benchmarking extends JobExecution {
  var executionTime = mutable.HashMap[UUID, Long]()
  var benchmarkActor: Option[ActorRef]

  abstract override def preHandleOperations(job: Job, master: ActorRef) = {
    startBenchmark(job)
    super.preHandleOperations(job,master)
  }

  abstract override def preFinishOperations(job: Job, results: ResultArray) = {
    sendBenchmarkResult(job)
    super.preFinishOperations(job, results)

  }

  def startBenchmark(job: Job) = {
    benchmarkActor.foreach(
      benchmarkingEnabled ⇒ {
        if(job.jobID.equals(job.parentCall)) benchmarkingEnabled ! ParentJobReceived(job.jobID, job.getClass.getSimpleName)
        executionTime.put(job.jobID, time)
      }

    )
  }

  def sendBenchmarkResult(job: Job): Unit = {
    benchmarkActor.foreach({
      val start: Long = executionTime.remove(job.jobID).getOrElse(0L)
      val result = BenchmarkResult(job.jobID, job.parentCall, job.getClass.getSimpleName, this.getClass.getSimpleName ,start, time)
      _ ! result
    })
  }


}

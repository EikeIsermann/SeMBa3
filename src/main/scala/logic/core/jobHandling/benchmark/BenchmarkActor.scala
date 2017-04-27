package logic.core.jobHandling.benchmark

import java.text.SimpleDateFormat
import java.util.{Calendar, UUID}

import akka.actor.{Actor, Props}
import globalConstants.{SembaPaths, SembaPresets}
import logic.core.jobHandling.Job
import utilities.{XMLExportable, XMLFactory}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.xml.Elem

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
case class BenchmarkResult(job: UUID, parentJob: UUID, description: String, atActor: String, start: Long, finish: Long) extends XMLExportable {
  override def toXML: Elem = {
     <BenchmarkResult>
       <JobID>{job.toString}</JobID>
       <JobDescription>{description}</JobDescription>
       <MeasuredAt>{atActor}</MeasuredAt>
       <AcceptedAt>{start}</AcceptedAt>
       <FinishedAt>{finish}</FinishedAt>
       <TimeInMillis>{(finish-start)/1000000}</TimeInMillis>
     </BenchmarkResult>
  }
}
case class ParentJobReceived(parentJob: UUID, description: String)
case class WriteResults(uri: String)

class BenchmarkActor extends Actor {
  val results = new mutable.HashMap[UUID, ArrayBuffer[BenchmarkResult]]()
  val parentJobs = ArrayBuffer[ParentJobReceived]()
  override def receive: Receive = {
    case p: ParentJobReceived => {
      if(!parentJobs.contains(p)) {
        results.put(p.parentJob, ArrayBuffer[BenchmarkResult]())
        parentJobs += p
      }
    }

    case b: BenchmarkResult ⇒
      {
        results.apply(b.parentJob) += b
      }
    case s: WriteResults ⇒ sender ! writeResults(s.uri)
  }

  def writeResults(uri: String): Boolean = {
    val now = Calendar.getInstance().getTime
    val sdf = new SimpleDateFormat()
    val saveTime = sdf.format(now)
    val xml = {
       <Benchmark date={saveTime}>
         {parentJobs.map(job => {
         <Job name={job.description} id={job.parentJob.toString}>
         {results.apply(job.parentJob).map(result => result.toXML)}
         </Job>
       })}
       </Benchmark>
    }
    XMLFactory.save(xml, uri)
     true
  }

  override def postStop(): Unit = {
    println("Poststoooop")
    writeResults("/Users/uni/desktop/shutdownBench.xml")
  }
}

object BenchmarkActor {
  def props = Props[BenchmarkActor]
}


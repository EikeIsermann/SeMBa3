package core.library

import java.io.{File, FileOutputStream}
import java.net.URI

import akka.actor.{Actor, Props}
import app.SembaPaths
import core._
import org.apache.jena.ontology.OntModel
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.shared.Lock
import utilities.{FileFactory, WriterFactory}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */


case class CreateCollection(name: String, classURI: String, libInfo: LibInfo, picture: URI) extends JobProtocol

class CollectionHandler extends Actor with JobHandling {
  override def receive: Receive = {
    case jobProtocol: JobProtocol => {

      acceptJob(jobProtocol, context.sender())
      jobProtocol match {
        case job: CreateCollection => {
          createCollection(job)
        }
      }
      self ! JobReply(jobProtocol)
    }

    case jobReply: JobReply => {
      handleReply(jobReply, self)
    }

  }

  def createCollection(job: CreateCollection): Unit = {
    val newLocation = WriterFactory.createFolder(job.libInfo.libraryLocation, job.name)
    WriterFactory.writeFile(new File(job.picture), newLocation)
    val uri = FileFactory.getURI(newLocation.toURI.toString) + "/" + job.libInfo.config.ontName
    val ont = setupOntology(job, uri)
    job.libInfo.libAccess ! createJob(RegisterOntology(uri, ont), job)
  }

  def setupOntology(job: CreateCollection, uri: String): OntModel = {
    val network = ModelFactory.createOntologyModel()
    val ont = network.createOntology(uri)
    val baseModel = job.libInfo.basemodel()
    baseModel.enterCriticalSection(Lock.READ)
    try {
      ont.addImport(baseModel.getOntology(job.libInfo.config.baseOntologyURI))
      network.addSubModel(job.libInfo.basemodel())
      network.setNsPrefix("base", job.libInfo.config.baseOntologyURI+"#")
      val pre = network.setNsPrefix("resource", uri +"#")

      val ontItem = network.createIndividual(pre + job.libInfo.config.itemName, network.getOntClass(job.classURI.toString))
      val readMetadataProperties = mutable.HashMap[String, Array[String]]()
      readMetadataProperties.put(SembaPaths.sembaTitle, Array(job.name))

      job.libInfo.libAccess !
        createJob(SetDatatypeProperties(readMetadataProperties, ontItem, network), job)
    }
    finally baseModel.leaveCriticalSection()
    network
  }

  override def handleJob(jobProtocol: JobProtocol): JobReply = ???
}

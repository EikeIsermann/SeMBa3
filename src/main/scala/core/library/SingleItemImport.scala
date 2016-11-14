package core.library

import java.io.{File, FileInputStream, FileNotFoundException, FileOutputStream}
import java.net.{URI, URLEncoder}
import java.util.UUID

import akka.actor.{Actor, PoisonPill, Props}
import akka.actor.Actor.Receive
import akka.agent.Agent
import app.Paths
import core.metadata.{PicThumb, ThumbActor}
import core._
import org.apache.jena.ontology.{Individual, OntModel, OntProperty}
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.shared.Lock
import utilities.{TextFactory, WriterFactory}
import utilities.debug.DC
import foo.bar.HelloReply
import org.apache.poi.hslf.blip.PICT
import org.apache.tika
import org.apache.tika.Tika
import org.apache.tika.metadata.{Metadata, TikaCoreProperties}
import org.apache.tika.parser.{AutoDetectParser, ParseContext}
import org.apache.tika.sax.BodyContentHandler

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.duration._
/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */

case class ImportNewItem(item: File, libInfo: LibInfo, copyToLib: Boolean) extends JobProtocol

class SingleItemImport extends Actor with JobHandling {
  var item: File = _
  var job: ImportNewItem = _
  var rootFolder: File = _
  var itemOntology: OntModel = _
  var itemIndividual: Individual = _
  def thumbLocation: String = rootFolder.toURI.toString + job.libInfo.config.thumbnail
  def uri: String = rootFolder.toURI.toString + job.libInfo.config.ontName


  override def receive: Receive = {
    case importJob: ImportNewItem => {
        originalSender.put(importJob, context.sender())
        job = importJob
        createJob(job, job)
      startImport()

    }

    case reply: JobReply => handleReply(reply)
  }

  def startImport(): Unit = {
    rootFolder = createFileStructure()
    itemOntology = setupOntology()
    analyzeItem()
    LibraryAccess.writeModel(itemOntology, new URI(uri))
    //context.actorOf(Props[PicThumb]) ! ThumbnailJob(item, new URI(rootFolder.toURI + "thumbnail.jpeg"), 512 )
    job.libInfo.library.send( base => LibraryAccess.addToLib(base, ArrayBuffer(itemOntology)))
    self ! JobReply(job)

  }

  def analyzeItem(): Unit =
  {
    val tika = new Tika()
    val parser = new AutoDetectParser()
    val metadata = new Metadata()
    val stream: FileInputStream = new FileInputStream(item)
    parser.parse(stream,
                new org.xml.sax.helpers.DefaultHandler(),
                metadata
                )
     for (metadataProperty <- metadata.names)
     {
       val prop = LibraryAccess.generateDatatypeProperty(metadataProperty, job.libInfo.basemodel(), job.libInfo.config.baseOntologyURI+"#")
       LibraryAccess.setDatatypeProperty(prop, itemOntology, itemIndividual, metadata.getValues(metadataProperty))
     }

    if(metadata.names().contains(TikaCoreProperties.TITLE)){
      LibraryAccess.setDatatypeProperty(Paths.sembaTitle,
        job.libInfo.basemodel(), itemIndividual, metadata.getValues(TikaCoreProperties.TITLE))
    }
    else{
      LibraryAccess.setDatatypeProperty(Paths.sembaTitle,
        job.libInfo.basemodel(), itemIndividual, Array(TextFactory.omitExtension(job.item.getAbsolutePath)))
    }

    val mimeType = tika.detect(item)
    println(mimeType)
    context.actorOf(ThumbActor.getProps(mimeType)) ! createJob(ThumbnailJob(item,rootFolder.toURI, job.libInfo.config), job)
    stream.close()
  }


  def createFileStructure(): File = {


    if (!job.item.exists()){
      DC.error(job.item.getPath + ": File not found.")
      new FileNotFoundException()
    }
    val newLocation =
      WriterFactory.createFolder( job.libInfo.libraryLocation, TextFactory.sanitizeFilename
      (TextFactory.omitExtension( job.item.getAbsolutePath )))
    if (job.copyToLib){
      item = WriterFactory.writeFile(job.item, newLocation)
    }
    newLocation
  }

  def setupOntology(): OntModel = {


    val network = ModelFactory.createOntologyModel()

    val ont = network.createOntology(uri)
    job.libInfo.basemodel().enterCriticalSection(Lock.READ)
    try {
      ont.addImport(job.libInfo.basemodel().getOntology(job.libInfo.config.baseOntologyURI))
      network.addSubModel(job.libInfo.basemodel())
    itemIndividual = network.createIndividual(uri + job.libInfo.config.itemName, network.getOntClass(Paths.itemClassURI))
    itemIndividual.addProperty(network.getProperty(job.libInfo.config.sourceLocation), item.toURI.toString)
      itemIndividual.addProperty(network.getProperty(Paths.thumbnailLocationURI), thumbLocation)
    }
    finally job.libInfo.basemodel().leaveCriticalSection()

    network
  }

  override def handleJob(jobProtocol: JobProtocol): JobReply = {
    JobReply(jobProtocol)
  }
}

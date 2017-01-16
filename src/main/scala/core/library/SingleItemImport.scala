package core.library

import java.io.{File, FileInputStream, FileNotFoundException}
import java.net.URI

import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import app.{Application, SembaPaths}
import core._
import core.metadata.ThumbActor
import org.apache.jena.ontology.{Individual, OntModel, OntModelSpec}
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.shared.Lock
import org.apache.tika.Tika
import org.apache.tika.metadata.{Metadata, TikaCoreProperties}
import org.apache.tika.parser.AutoDetectParser
import sembaGRPC._
import utilities.debug.DC
import utilities.{Convert, FileFactory, TextFactory, WriterFactory}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */

case class ImportNewItem(item: File, libInfo: LibInfo, copyToLib: Boolean) extends JobProtocol
case class GenerateDatatypeProperties(keys: scala.collection.Set[String], model: OntModel) extends JobProtocol
case class SetDatatypeProperties(propertyMap: mutable.HashMap[String, Array[String]],
                                 item: Individual, model: OntModel) extends JobProtocol

// TODO ScalaDoc, Custom Metadata Mappings
class SingleItemImport extends Actor with JobHandling {
  var item: File = _
  var job: ImportNewItem = _
  var rootFolder: File = _
  var itemOntology: OntModel = _
  var itemIndividual: Individual = _
  var title: String =_
  override def receive: Receive = {
    case jobProtocol: JobProtocol =>
      {
        acceptJob(jobProtocol, context.sender())

        jobProtocol match {
          case importJob: ImportNewItem => {
            acceptJob(importJob, sender())
            job = importJob
            startImport()

          }

        }
        self ! JobReply(jobProtocol)
      }


    case reply: JobReply => handleReply(reply, self)
  }

  override def handleReply(reply: JobReply, selfRef: ActorRef): Boolean = {
    reply.job match {
      case sDP: SetDatatypeProperties =>  {
        job.libInfo.libAccess ! createJob(RegisterOntology(itemIndividual.getURI, itemOntology), job)
      }
      case _ =>
    }
    super.handleReply(reply, selfRef)
  }

  def startImport(): Unit = {
    rootFolder = createFileStructure()
    itemOntology = setupOntology()
    analyzeItem()
    updates.apply(job.jobID).+=(
      UpdateMessage(kindOfUpdate = UpdateType.ADD, lib = job.libInfo.libURI).addItems(
      Resource().withLib(Library(job.libInfo.libURI))
        .withUri(uri)
        .withItemType(ItemType.ITEM)
        .withName(title)
        .withThumbnailLocation(thumbLocation)
    )

    )
  }


  def analyzeItem(): Unit = {
    val tika = new Tika()
    val parser = new AutoDetectParser()
    val metadata = new Metadata()
    val stream: FileInputStream = new FileInputStream(item)
    val mimeType = tika.detect(item)
   ThumbActor.getThumbActor(mimeType) ! createJob(ThumbnailJob(item, rootFolder.toURI, job.libInfo.config), job)

    parser.parse(stream,
      new org.xml.sax.helpers.DefaultHandler(),
      metadata
    )

    val readMetadataProperties = mutable.HashMap[String, Array[String]]()
    for (metadataProperty <- metadata.names) {
      val metaURI = job.libInfo.config.baseOntologyURI + "#" + TextFactory.cleanString(metadataProperty)
      readMetadataProperties.put(metaURI, metadata.getValues(metadataProperty))
    }
    job.libInfo.libAccess !
      createJob(GenerateDatatypeProperties(readMetadataProperties.keySet, job.libInfo.basemodel()),job)
    title = Option(metadata.get(TikaCoreProperties.TITLE)).getOrElse(TextFactory.omitExtension(job.item.getAbsolutePath))

    readMetadataProperties.put(SembaPaths.sembaTitle, Array(title))

    job.libInfo.libAccess !
      createJob(SetDatatypeProperties(readMetadataProperties, itemIndividual, itemOntology), job)

    stream.close()
  }

  def createFileStructure(): File = {


    if (!job.item.exists()) {
      DC.error(job.item.getPath + ": File not found.")
      new FileNotFoundException()
    }
    val newLocation =
      WriterFactory.createFolder(job.libInfo.libraryLocation, TextFactory.sanitizeFilename
      (TextFactory.omitExtension(job.item.getAbsolutePath)))
    if (job.copyToLib) {
      item = WriterFactory.writeFile(job.item, newLocation)
    }
    newLocation
  }

  def setupOntology(): OntModel = {


    val network = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM)

    val ont = network.createOntology(uri)
    job.libInfo.library().enterCriticalSection(Lock.READ)
    job.libInfo.basemodel().enterCriticalSection(Lock.READ)
    try {
      //ont.addImport(job.libInfo.basemodel().getOntology(job.libInfo.config.baseOntologyURI))
      //network.addSubModel(job.libInfo.basemodel())
      network.setNsPrefix("base", job.libInfo.config.baseOntologyURI+"#")
      network.setNsPrefix("resource", uri + "#")
      itemIndividual = network.createIndividual(network.getNsPrefixURI("resource") + job.libInfo.config.itemName, network.getOntClass(SembaPaths.itemClassURI))
      itemIndividual.addProperty(network.getProperty(job.libInfo.config.sourceLocation), item.toURI.toString)
      itemIndividual.addProperty(network.getProperty(SembaPaths.thumbnailLocationURI), thumbLocation)
    }
    finally {job.libInfo.basemodel().leaveCriticalSection() ;job.libInfo.library().leaveCriticalSection()}
    network
  }

  def thumbLocation: String = rootFolder.toURI.toString + job.libInfo.config.thumbnail

  def uri: String = FileFactory.getURI(rootFolder.toURI.toString) + "/" + job.libInfo.config.ontName

  override def handleJob(jobProtocol: JobProtocol): JobReply = {
    JobReply(jobProtocol)
  }
}

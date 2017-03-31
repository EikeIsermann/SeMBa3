package logic.core

import java.io.File
import java.net.URI
import java.nio.file.{Files, Paths}

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorRef, ActorSystem, Props, Stash}
import akka.agent.Agent
import akka.util.Timeout
import api._
import app.Application
import data.storage.{FileRemover, RemoveFromOntology}
import logic._
import logic.dataExport.DataExport
import logic.resourceCreation.ResourceCreation
import org.apache.jena.ontology.{Individual, OntModel}
import org.apache.jena.query.{Dataset, ReadWrite}
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.tdb.TDBFactory
import sembaGRPC._
import utilities.SembaConstants.StorageSolution
import utilities.debug.DC
import utilities.{Convert, XMLFactory}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import globalConstants._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Created by Eike on 06.04.2016.
  */

case class LibInfo(system: ActorSystem, constants: Config, libAccess: ActorRef, libURI: String, rootFolder: String)
abstract class SembaBaseActor(val root: String) extends Actor with Stash with Initialization with ActorFeatures with JobHandling
{
  val libRoot = new URI(root)
  val config: Config = initializeConfig()
  val system = context.system
  initializeConfig()
  val libraryLocation: URI = new URI(libRoot + config.dataPath)

  def initializeConfig(): Config
  def libInfo(): LibInfo

}

class Semba(root: String) extends SembaBaseActor(root) with AccessToStorage with ResourceCreation //with DataExport
{


  def initializeConfig(): Config = {
    val libConfig = Paths.get(new URI(libRoot + SembaPaths.libConfiguration))
    if(!Files.exists(libConfig))
    {
       Files.copy(Paths.get("/src", "resources", "config.xml"), Paths.get(libRoot))
    }
    //TODO update baseOntologyURI to current path!
    new Config(libConfig.toUri, root)
  }

  override def wrappedReceive: Receive =
  {

    case jobReply: JobReply => {
      handleReply(jobReply)


    }
    case _ => {
    }
  }
  def libInfo: LibInfo = LibInfo(system, config, queryExecutor, config.baseOntologyURI, root)

  /*
  override def processUpdates(jobProtocol: JobProtocol): Option[ArrayBuffer[UpdateMessage]] = {
    updates(jobProtocol.jobID).foreach(update => Application.api ! update)
  }



  def removeItem(resource: Resource): VoidResult = {
    system.actorOf(Props[FileRemover]) ! createMasterJob(RemoveFromOntology(resource, libAccess), self)
    VoidResult().withAccepted(true)
  }

  def getConcepts(): LibraryConcepts = {
    LibraryAccess.retrieveLibConcepts(basemodel()).withLib(Convert.lib2grpc(path))
  }

  def getContents(): LibraryContent = {
    LibraryAccess.retrieveLibContent(ontology, Library(path))
  }

  def getMetadata(item: String): ItemDescription = {
    LibraryAccess.retrieveMetadata(item, ontology)
  }
   */

}

class Config(path: URI,  root: String) {
  val rootFolder = root
  val configFile = XMLFactory.getXMLAsElemAdv(path)
  val name = getString("libraryName")
  val baseOntologyURI = getString("baseOntologyURI")
  val resourceBaseURI = baseOntologyURI + "#"
  val itemClassURI = getString("itemClassURI")
  val defaultCollectionIcon = getString("defaultCollectionIcon")
  val ontName = getString("ontName")
  val itemName = getString("itemName")
  val thumbResolution = getString("thumbnailResolution")
  val thumbnail = getString("thumbnailIdentifier")
  val dataPath = getString("dataPath")
  val storagePath = getString("libraryPath")
  val storageType = StorageSolution.withName(getString("storageType"))
  val temp = getString("temporaryFolder")
  val language = getString("language")
  val lang = getString("language")


  def getString(s: String): String = configFile.getValueAt("config", s)


}




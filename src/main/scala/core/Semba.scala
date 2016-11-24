package core

import java.net.URI
import java.util.UUID

import akka.pattern.ask
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.agent.Agent
import akka.routing.RoundRobinPool
import akka.util.Timeout
import api._
import app.{Application, Paths}
import core.library._
import core.{LibraryAccess => lib}
import org.apache.jena.ontology.{Individual, OntModel, OntModelSpec}
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.util.{FileUtils, URIref}
import sembaGRPC.SourceFile.Source
import sembaGRPC._
import utilities.debug.DC
import utilities.{Convert, FileFactory, XMLFactory}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Created by Eike on 06.04.2016.
  */

case class LibInfo(system: ActorSystem, library: Agent[OntModel], basemodel: Agent[OntModel], libraryLocation: URI, config: Config, libAccess: ActorRef, libURI: String)

class Semba(val path: String) extends Actor with JobHandling {
  var system = context.system
  var library: Agent[OntModel] = _
  var basemodel: Agent[OntModel] = _
  var libraryLocation: URI = _
  var libRoot: URI = _
  var config: Config = _
  var libAccess: ActorRef = _
  var libInitialized: Future[JobReply] = _
  var resourceCreation: ActorRef = _
  implicit val timeout = Timeout(300 seconds)


  override def preStart() = {
    library = Agent(ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM))
    libAccess = system.actorOf(Props(new CriticalOntologyAccess(library, path)))
    basemodel = Agent(ModelFactory.createOntologyModel())
    Await.result(basemodel.alter(base => lib.load(base, path)), 10 second)
    libRoot = new URI(getLiteral(basemodel().getIndividual(path + "#LibraryDefinition"), Paths.libraryRootFolder))
    config = new Config(new URI(libRoot + Paths.libConfiguration))
    libraryLocation = new URI(libRoot + config.dataPath)
    libInitialized = ask(system.actorOf(Props(new LibImporter(library))), ImportLib(libraryLocation, libInfo)).mapTo[JobReply]
    resourceCreation = context.actorOf(Props(new ResourceCreation(libInfo)))
  }

  override def processUpdates(jobProtocol: JobProtocol): Option[ArrayBuffer[UpdateMessage]] = {
    updates(jobProtocol.jobID).foreach(update => Application.api ! update)
    super.processUpdates(jobProtocol)
  }

  def readLibrary(): Unit = {

  }

  def removeCollectionItem(collectionItem: CollectionItem): Any = {

  }

  def removeItem(resource: Resource): VoidResult = {
    system.actorOf(Props[FileRemover]) ! createMasterJob(RemoveFromOntology(resource, libAccess), self)
    VoidResult().withAccepted(true)
  }

  def createRelation(relationModification: RelationModification): Any = {

  }

  def updateMetadata(updateMeta: UpdateMetadata): Any = {

  }

  def removeRelation(relationModification: RelationModification): Any = {

  }

  override def receive: Receive = {
    case apiCall: SembaApiCall => {
      apiCall match {
        case openLib: OpenLib => {
          println("Waiting for result")
          val test = Await.result(libInitialized, timeout.duration)
          println("Result")
          sender() ! getConcepts()
        }
        case addItem: AddToLibrary => {
          val notEmpty = addItem.sourceFile.source.isDefined
          sender() ! VoidResult(notEmpty, if (notEmpty) "Trying to import Item." else "No source file set.")
          resourceCreation ! createMasterJob(addItem, self)
        }

        case contents: RequestContents => {
          sender() ! getContents()
        }

        case getMeta: GetMetadata => {
          sender() ! getMetadata(getMeta.resource.uri)
        }
        case removeIt: RemoveFromLibrary => {
          sender() ! removeItem(removeIt.resource)
        }
        case updateMeta: UpdateMetadata => {
          sender() ! updateMetadata(updateMeta)
        }

        case removeCollItem: RemoveCollectionItem => {
          sender() ! removeCollectionItem(removeCollItem.collectionItem)
        }

        case createRel: CreateRelation => {
          sender() ! createRelation(createRel.relationModification)
        }
        case removeRel: RemoveRelation => {
          sender() ! removeRelation(removeRel.relationModification)
        }
        case sparql: SparqlFilter => {
          sender() ! LibraryContent()
          system.actorOf(Props(new Search(library()))) ! sparql

        }

      }
    }




    case jobReply: JobReply => {
      handleReply(jobReply, self)


    }
    case _ => {
    }
  }

  def getLiteral(item: Individual, str: String): String = {
    try {
      val prop = library().getProperty(str)
      item.getPropertyValue(prop).toString
    }
    catch {
      case ex: Exception =>
        DC.warn("Couldn't retrieve literal")
    }
  }



  def libInfo: LibInfo = LibInfo(system, library, basemodel, libraryLocation, config, libAccess, path)

  def getConcepts(): LibraryConcepts = {
    LibraryAccess.retrieveLibConcepts(basemodel()).withLib(Convert.lib2grpc(path))
  }

  def getContents(): LibraryContent = {
    LibraryAccess.retrieveLibContent(library(), Library(path))
  }

  def getMetadata(item: String): ItemDescription = {
    LibraryAccess.retrieveMetadata(item, library())
  }

  override def handleJob(jobProtocol: JobProtocol): JobReply = ???
}

class Config(path: URI) {
  val configFile = XMLFactory.getXMLAsElemAdv(path)

  val baseOntologyURI = getString("baseOntologyURI")

  val itemClassURI = getString("itemClassURI")

  val sourceLocation = getString("sourceLocation")

  val defaultCollectionIcon = getString("defaultCollectionIcon")

  val ontName = getString("ontName")

  val itemName = getString("itemName")

  val thumbResolution = getString("thumbnailResolution")

  val lang = getString("language")
  val thumbnail = getString("thumbnailIdentifier")
  val dataPath = getString("dataPath")

  def getString(s: String): String = configFile.getValueAt("config", s)


}




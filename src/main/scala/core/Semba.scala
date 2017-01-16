package core

import java.io.File
import java.net.URI
import java.nio.file.{Files, Paths}
import java.util.UUID

import akka.pattern.ask
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.agent.Agent
import akka.routing.RoundRobinPool
import akka.util.Timeout
import api._
import app.{Application, SembaPaths}
import core.library._
import org.apache.jena.ontology.{Individual, OntModel, OntModelSpec}
import org.apache.jena.query.{Dataset, ReadWrite}
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.tdb.TDBFactory
import org.apache.jena.tdb.base.file.Location
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

case class LibInfo(system: ActorSystem, library: Dataset, basemodel: Agent[OntModel], libraryLocation: URI, config: Config, libAccess: ActorRef, libURI: String)

class Semba(val root: String) extends Actor with JobHandling {
  var system = context.system
  var basemodel: Agent[OntModel] = _
  var ontology: Dataset = _
  var libraryLocation: URI = _
  var libRoot: URI = _
  var config: Config = _
  var libAccess: ActorRef = _
  var libInitialized: Future[JobReply] = _
  var resourceCreation: ActorRef = _
  var path: String = _
  implicit val timeout = Timeout(300 seconds)


  override def preStart() = {
    initializeConfig()
    libRoot = new URI(root)
    config = new Config(new URI(root + SembaPaths.libConfiguration))
    path = config.baseOntologyURI
    initializeOntology()
    libRoot  = new URI(root)
    config = new Config(new URI(libRoot + SembaPaths.libConfiguration))
    libraryLocation = new URI(libRoot + config.dataPath)
    //libInitialized = ask(system.actorOf(Props(new LibImporter(ontology))), ImportLib(libraryLocation, libInfo)).mapTo[JobReply]
    resourceCreation = context.actorOf(Props(new ResourceCreation(libInfo)))


  }
  def initializeOntology() = {
    val loc = new File(new URI(libRoot + config.tdbPath)).getAbsolutePath
    libAccess = system.actorOf(Props(new CriticalStorageAccess(ontology, path)))
    ontology = TDBFactory.createDataset(loc)
    basemodel = Agent(ModelFactory.createOntologyModel())
    if(!Files.exists(Paths.get(new URI(path))))
    {
      basemodel().createOntology(path)
      basemodel().addLoadedImport(SembaPaths.mainUri)
      basemodel().setNsPrefix(SembaPaths.mainUri, "main")
      basemodel().setNsPrefix(path, "")
    }
    else
    {
      Await.result(basemodel.alter(base => lib.load(base, path)), 10 second)
    }


    ontology.begin(ReadWrite.WRITE)
    try{
      ontology.addNamedModel(path, basemodel())
      ontology.commit()
    }
    finally ontology.end()


  }

  def initializeConfig(): Unit ={
    val libConfig = Paths.get(new URI(libRoot + SembaPaths.libConfiguration))
    if(!Files.exists(libConfig))
    {
       Files.copy(Paths.get("/src", "resources", "config.xml"), Paths.get(new URI(path)))
    }

    //TODO update baseOntologyURI to current path!
    config = new Config(new URI(root + SembaPaths.libConfiguration))
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
          val time = System.currentTimeMillis()
          println("Loading Library")
          val test = Await.result(libInitialized, timeout.duration)
          println("Loading took: " + (System.currentTimeMillis() - time)/1000)
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
          system.actorOf(Props(new Search(ontology))) ! sparql

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
      val prop = basemodel().getProperty(str)
      item.getPropertyValue(prop).toString
    }
    catch {
      case ex: Exception =>
        DC.warn("Couldn't retrieve literal")
    }
  }



  def libInfo: LibInfo = LibInfo(system, ontology, basemodel, libraryLocation, config, libAccess, path)

  def getConcepts(): LibraryConcepts = {
    LibraryAccess.retrieveLibConcepts(basemodel()).withLib(Convert.lib2grpc(path))
  }

  def getContents(): LibraryContent = {
    LibraryAccess.retrieveLibContent(ontology, Library(path))
  }

  def getMetadata(item: String): ItemDescription = {
    LibraryAccess.retrieveMetadata(item, ontology)
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
  val tdbPath = getString("libraryPath")


  def getString(s: String): String = configFile.getValueAt("config", s)


}




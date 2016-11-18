package core

import java.net.URI
import java.util.UUID

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.agent.Agent
import akka.routing.RoundRobinPool
import api._
import app.Paths
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
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Created by Eike on 06.04.2016.
  */

case class LibInfo(system: ActorSystem, library: Agent[OntModel], basemodel: Agent[OntModel], libraryLocation: URI, config: Config)

class Semba(val path: String) extends Actor with JobHandling {
  var system = context.system
  var library: Agent[OntModel] = _
  var basemodel: Agent[OntModel] = _
  var libraryLocation: URI = _
  var libRoot: URI = _
  var config: Config = _

  override def preStart() = {
    library = Agent(ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM))
    basemodel = Agent(ModelFactory.createOntologyModel())
    Await.result(basemodel.alter(base => lib.load(base, path)), 1000 second)
    var test = basemodel().listIndividuals().toList
    libRoot = new URI(getLiteral(basemodel().getIndividual(path + "#LibraryDefinition"), Paths.libraryRootFolder))
    config = new Config(new URI(libRoot + Paths.libConfiguration))
    libraryLocation = new URI(libRoot + config.dataPath)
    init()
  }

  def init(): Unit = {
    readLibrary()
  }

  /*
  for(_ <- 1 to 1000)  {
    val time = System.currentTimeMillis()
    library().enterCriticalSection(Lock.READ)
    try{
      println(library().listIndividuals(library().getOntClass("http://www.hci.uni-wuerzburg.de/ontologies/semba/semba-main.owl#MediaItem")).toList.size() + " items took "+ (System.currentTimeMillis() - time))
    }
    finally library().leaveCriticalSection()
    //println(job.libInfo.library())
    //println(job.libInfo.library().listIndividuals(job.libInfo.library().getOntClass("http://www.hci.uni-wuerzburg.de/ontologies/semba/semba-main.owl#MediaItem")).toList.size() + " items took "+ (System.currentTimeMillis() - time))

  }
    */

  //newCollection("testCollection", new URI("http://www.hci.uni-wuerzburg.de/ontologies/semba/semba-teaching.owl#Program") )

  def readLibrary(): Unit = {
    system.actorOf(Props(new LibImporter(library))) ! ImportLib(libraryLocation)
  }

  def removeCollectionItem(collectionItem: CollectionItem): Any = {

  }

  def removeItem(resource: Resource): VoidResult = {
    system.actorOf(Props[FileRemover]) ! createMasterJob(RemoveFromOntology(resource.uri,library()), self)
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
          sender() ! getConcepts()
        }
        case addItem: AddToLibrary => {
          val notEmpty = addItem.sourceFile.source.isDefined
          sender() ! VoidResult(notEmpty, if (notEmpty) "Trying to import Item." else "No source file set.")
          newItem(addItem)
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


      }
    }




    case jobReply: JobReply => {
      if (handleReply(jobReply)){
         basemodel.send(base => LibraryAccess.writeModel(base))
      }

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


  def newItem(addToLibrary: AddToLibrary): Unit = {
    addToLibrary.sourceFile.source match {
      case Source.Path(path) => {
        newItem(FileFactory.getURI(path))
      }

      case Source.Data(data) => {
        //TODO get input stream, copy to local filesystem, read as new item
      }

      case Source.Coll(newColl) =>{
        newCollection(newColl.name , newColl.ontClass )
      }
      case Source.Empty =>
    }

  }

  def newItem(path: String, copyToLib: Boolean = true): Unit = {
    val items = FileFactory.contentsOfDirectory(new URI(path), true, false, false)
    val workers = system.actorOf(new RoundRobinPool(10).props(Props[SingleItemImport]))
    val batchID = UUID.randomUUID()
    for (item <- items) {
      val job = ImportNewItem(item, libInfo, copyToLib)
      job.jobID = batchID
      workers ! createMasterJob(job, self)
    }
  }

  def libInfo: LibInfo = LibInfo(system, library, basemodel, libraryLocation, config)

  def getConcepts(): LibraryConcepts = {
    LibraryAccess.retrieveLibConcepts(basemodel()).withLib(Convert.lib2grpc(path))
  }

  def getContents(): LibraryContent = {
    LibraryAccess.retrieveLibContent(library(), Library(path))
  }

  def getMetadata(item: String): ItemDescription = {
    LibraryAccess.retrieveMetadata(item, library())
  }

  def newCollection(name: String, classURI: String, picture: URI = new URI(config.defaultCollectionIcon)): Unit = {
    system.actorOf(Props[CollectionHandler]) ! createMasterJob(CreateCollection(name, classURI, libInfo, picture), self)

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




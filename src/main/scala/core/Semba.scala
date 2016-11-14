package core

import java.io.{File, FileOutputStream}
import java.net.URI
import java.util.UUID

import akka.actor.Actor.Receive
import core.{LibraryAccess => lib}
import akka.actor.{Actor, ActorContext, ActorSystem, Props}
import akka.agent.Agent
import akka.routing.RoundRobinPool
import akka.util.LineNumbers.SourceFile
import api.{AddToLibrary, OpenLib, RemoveCollectionItem, RequestContents}
import app.Paths
import core.library._
import org.apache.jena.ontology.{Individual, OntModel, OntModelSpec}
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.shared.Lock
import org.apache.jena.vocabulary.DCTerms
import sembaGRPC.{LibraryConcepts, LibraryContent, VoidResult}
import sembaGRPC.SourceFile.Source
import utilities.{Convert, FileFactory, XMLFactory}
import utilities.debug.DC

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
/**
  * Created by Eike on 06.04.2016.
  */

case class LibInfo(system: ActorSystem, library: Agent[OntModel], basemodel: Agent[OntModel], libraryLocation: URI, config: Config)
class Semba(val path: URI) extends Actor with JobHandling
{
  var system = context.system
  var library: Agent[OntModel] = _
  var basemodel: Agent[OntModel] = _
  var libraryLocation: URI = _
  var config: Config = _
  def libInfo: LibInfo = new LibInfo(system, library, basemodel, libraryLocation, config)

  override def preStart() = {
       library =  Agent(ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM))
       basemodel = Agent(ModelFactory.createOntologyModel())
      Await.result(basemodel.alter(base => lib.load(base, path)), 1000 second)
      libraryLocation = new URI(getLiteral(basemodel().getIndividual("http://www.hci.uni-wuerzburg.de/ontologies/semba/semba-teaching.owl#LibraryDefinition"), "http://www.hci.uni-wuerzburg.de/ontologies/semba/semba-main.owl#libraryRootFolder"))
       config = new Config("/Users/uni/Documents/SeMBa3/appdata/libraries/Config.xml")//libraryLocation  +  Paths.libConfiguration)
      init()
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


  def init(): Unit = {
    readLibrary()
  }


  override def receive: Receive = {
    case openLib: OpenLib => {
      sender() ! getConcepts()
    }
    case addItem: AddToLibrary => {
        val notEmpty = addItem.sourceFile.source.isDefined
        sender() ! VoidResult(notEmpty, if (notEmpty) "Trying to import Item." else "No source file set." )
        newItem(addItem)
      }

    case contents: RequestContents => {
      sender() ! getContents()
    }

    case JobReply => {}
    case _ => {
      println()
    }
  }

  def readLibrary(): Unit = {
    system.actorOf(Props(new LibImporter(library))) ! ImportLib(libraryLocation)
  }


  def newItem (addToLibrary: AddToLibrary): Unit ={
     addToLibrary.sourceFile.source match {
       case Source.Path(path) =>
         {
           newItem(new URI(path))
         }

       case Source.Data(data) =>
         {
           //TODO get input stream, copy to local filesystem, read as new item
         }
     }
  }

  def newItem( path: URI , copyToLib: Boolean = true): Unit = {
    val items = FileFactory.contentsOfDirectory(path, true, false, false)
    val workers = system.actorOf(new RoundRobinPool(10).props(Props[SingleItemImport]))
    val batchID = UUID.randomUUID()
    for (item <- items){
      val job =   ImportNewItem(item, libInfo, copyToLib)
      job.jobID = batchID
      workers ! createMasterJob(job, self)
    }
  }

  def newCollection( name: String, classURI: URI,  picture: URI = new URI(config.defaultCollectionIcon)): Unit ={
    system.actorOf(Props[CollectionHandler]) ! CreateCollection(name, classURI, libInfo, picture)

  }

  def getLiteral(item:Individual, str: String): String = {
    try{
      val prop = library().getProperty(str)
      item.getPropertyValue(prop).toString
    }
    catch{
      case ex: Exception =>
        DC.warn("Couldn't retrieve literal")
    }
  }

  def getConcepts(): LibraryConcepts = {
    LibraryAccess.retrieveLibConcepts(basemodel()).withLib(Convert.lib2grpc(path.toString))
  }

  def getContents(): LibraryContent = {
     LibraryAccess.retrieveLibContent(library())
  }



  override def handleJob(jobProtocol: JobProtocol): JobReply = ???
}

class Config(path: String)
{
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
  def getString(s: String): String =  configFile.getValueAt("config", s)


}




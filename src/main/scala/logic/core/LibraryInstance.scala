package logic.core

import java.net.URI
import java.nio.file.{Files, Paths}

import akka.actor.{ActorRef, ActorSystem, Stash}
import logic.core.jobHandling.benchmark.{BenchmarkActor, Benchmarking, WriteResults}
import logic.resourceCreation.ResourceCreation
import utilities.SembaConstants.StorageSolution
import utilities.XMLFactory
import globalConstants._
import logic.core.jobHandling.{JobHandling, JobReply}
import logic.search.Search


/**
  * Created by Eike on 06.04.2016.
  */

case class Config(system: ActorSystem, constants: Constants, libAccess: ActorRef, libURI: String, rootFolder: String, benchmarkActor: Option[ActorRef])
abstract class SembaBaseActor(val root: String) extends Stash with Initialization with ActorFeatures with JobHandling
{

  val libRoot = Paths.get(new URI(root)).getParent.toUri// new URI(root)
  val constants: Constants = initializeConstants()
  val system = context.system
  initializeConstants()
  val libraryLocation: URI = new URI(libRoot + constants.dataPath)
  def config: Config
  def initializeConstants(): Constants

}

class LibraryInstance(root: String) extends SembaBaseActor(root) with AccessToStorage with ResourceCreation with Search with Benchmarking//with DataExport
{
  val benchDummy = Some(system.actorOf(BenchmarkActor.props))
  var benchmarkActor: Option[ActorRef] = if (constants.benchmarkingEnabled) benchDummy else None

  override def wrappedReceive: Receive = {
    case write: WriteResults ⇒ benchmarkActor.foreach( _ ! write)
  }

  def initializeConstants(): Constants = {
    val libConfig = Paths.get(new URI(libRoot + SembaPaths.libConfiguration))
    if(!Files.exists(libConfig))
    {
       Files.copy(Paths.get("/src", "resources", "config.xml"), Paths.get(libRoot))
    }
    //TODO update baseOntologyURI to current path!
    val retVal = new Constants(libConfig.toUri, libRoot.toString)
    val tempFolder = Paths.get(new URI(retVal.rootFolder + retVal.temp))
    if(!Files.exists(tempFolder)) Files.createDirectories(tempFolder)
    retVal
  }
   def config: Config = Config(system, constants, queryExecutor, constants.baseOntologyURI, libRoot.toString, benchmarkActor)

}

class Constants(path: URI, root: String) {
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
  val benchmarkPath = getString("benchmarkFile")
  val benchmarkingEnabled = getString("benchmarkingEnabled").toBoolean



  def getString(s: String): String = configFile.getValueAt("config", s)


}




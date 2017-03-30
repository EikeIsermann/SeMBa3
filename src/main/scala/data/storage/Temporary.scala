package data.storage

import java.io.{File, FileNotFoundException}

import globalConstants.SembaPaths
import logic.resourceCreation.{GenerateDatatypeProperties, SetDatatypeProperties}
import org.apache.jena.ontology.{OntModel, OntModelSpec}
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.shared.Lock
import org.apache.tika.metadata.TikaCoreProperties
import sembaGRPC._
import utilities.{FileFactory, TextFactory, WriterFactory}
import utilities.debug.DC

import scala.collection.mutable

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
     /*
trait Temporary {

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

  updates.apply(job.jobID).+=(
    UpdateMessage(kindOfUpdate = UpdateType.ADD, lib = job.libInfo.libURI).addItems(
      Resource().withLib(Library(job.libInfo.libURI))
        .withUri(uri)
        .withItemType(ItemType.ITEM)
        .withName(title)
        .withThumbnailLocation(thumbLocation)
    )


      job.libInfo.libAccess !
    createJob(GenerateDatatypeProperties(readMetadataProperties.keySet, job.libInfo.basemodel()),job)
  title = Option(metadata.get(TikaCoreProperties.TITLE)).getOrElse(TextFactory.omitExtension(job.item.getAbsolutePath))

  readMetadataProperties.put(SembaPaths.sembaTitle, Array(title))

  job.libInfo.libAccess !
    createJob(SetDatatypeProperties(readMetadataProperties, itemIndividual, itemOntology), job)



  def thumbLocation: String = rootFolder.toURI.toString + job.libInfo.config.thumbnail

  def uri: String = FileFactory.getURI(rootFolder.toURI.toString) + "/" + job.libInfo.config.ontName


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

  val newLocation = WriterFactory.createFolder(job.libInfo.libraryLocation, job.name)
  WriterFactory.writeFile(new File(job.picture), newLocation)
  val uri = FileFactory.getURI(newLocation.toURI.toString) + "/" + job.libInfo.config.ontName
  val ont = setupOntology(job, uri)
  job.libInfo.libAccess ! createJob(RegisterOntology(uri, ont), job)

}

def initializeOntology() = {
  val loc = new File(new URI(libRoot + config.tdbPath)).getAbsolutePath
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

  def addToLib(uri: String, parent: OntModel, child: OntModel): OntModel = {
  activeModels.send(map => map.+((uri,child)))
  parent.enterCriticalSection(Lock.WRITE)
  child.enterCriticalSection(Lock.WRITE)
  try {
  parent.addSubModel(child)
}
  finally {parent.leaveCriticalSection(); child.leaveCriticalSection()}
  parent
}

}
     */

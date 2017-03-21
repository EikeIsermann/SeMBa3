package data.storage

import java.io.{File, FileNotFoundException}

import app.SembaPaths
import org.apache.jena.ontology.{OntModel, OntModelSpec}
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.shared.Lock
import sembaGRPC._
import utilities.{FileFactory, TextFactory, WriterFactory}
import utilities.debug.DC

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
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



  def thumbLocation: String = rootFolder.toURI.toString + job.libInfo.config.thumbnail

  def uri: String = FileFactory.getURI(rootFolder.toURI.toString) + "/" + job.libInfo.config.ontName

}

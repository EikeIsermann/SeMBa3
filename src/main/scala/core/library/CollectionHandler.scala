package core.library

import java.io.{File, FileOutputStream}
import java.net.URI

import akka.actor.Actor
import akka.actor.Actor.Receive
import core.LibInfo
import org.apache.jena.ontology.OntModel
import org.apache.jena.rdf.model.ModelFactory
import utilities.WriterFactory

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */

sealed trait CollectionProtocol
case class CreateCollection(name: String, classURI: URI, libInfo: LibInfo, picture: URI ) extends CollectionProtocol

class CollectionHandler extends Actor {
  override def receive: Receive = {
    case job: CreateCollection => createCollection(job)
  }

  def createCollection(job: CreateCollection): Unit ={
    val newLocation = WriterFactory.createFolder(job.libInfo.libraryLocation, job.name)
    WriterFactory.writeFile(new File(job.picture), newLocation)
    val ont = setupOntology(job, newLocation.toURI.toString + job.libInfo.config.ontName)
  }

  def setupOntology(job: CreateCollection, uri: String): OntModel ={
    val network = ModelFactory.createOntologyModel()
    val ont = network.createOntology( uri  )

    ont.addImport(job.libInfo.basemodel().getOntology(job.libInfo.config.baseOntologyURI))
    network.addSubModel(job.libInfo.basemodel())
    val ontItem = network.createIndividual(uri + job.libInfo.config.itemName, network.getOntClass(job.classURI.toString))
    network.write(new FileOutputStream(new File(new URI(uri))),"TURTLE")
    network
  }
}

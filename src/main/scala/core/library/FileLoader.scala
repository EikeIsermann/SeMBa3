package core.library

import java.io.File

import akka.actor.Actor
import akka.actor.Actor.Receive
import app.Presets
import core.ontology.MediaLibrary
import org.apache.jena.ontology.OntModel
import org.apache.jena.rdf.model.ModelFactory
import utilities.FileFactory

import scala.collection.mutable.ArrayBuffer

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */


class FileLoader extends Actor {

  override def receive: Receive = {
    case LoadFolder(folder) => sender ! readFolder(folder)
  }


  def readFolder(dir: File): SuccessfulSingleImport = {
    var retVal = ArrayBuffer[OntModel]()
    for (source <- FileFactory.filterFileExtension(dir, Presets.validOntologyExtensions))
  {
     val ontology = ModelFactory.createOntologyModel()
     ontology.read(source.toString)
     retVal += ontology
  }
     SuccessfulSingleImport(retVal)
  }
}

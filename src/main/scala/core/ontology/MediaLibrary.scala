package core.ontology

import java.net.URI

import akka.agent.Agent
import org.apache.jena.ontology.OntModel
import org.apache.jena.rdf.model.{Model, ModelFactory}
import utilities.debug.DC

import scala.concurrent.{ExecutionContext, Future}

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
class MediaLibrary(path: String) {

  var network = ModelFactory.createOntologyModel()

  try
  {
    network.read(path)
  }

  catch {
    case ex: Exception => {
      DC.warn(ex.toString)
      DC.warn("Could not load Media Network, either erroneous or not existent.")
    }
  }


  def combineWith( partner: OntModel ): Unit = {
    network.addSubModel( partner )
  }

  def removeFromLibrary( ): Unit = {

  }


  def getItem( path: URI ): OntModel = {
   network.getImportedModel(path.toString)
  }

  def getLibraryDefinition( ): Model = {
    network.getBaseModel()
  }



  def addToCollection(collection: URI, item: URI): Unit = {

  }

  def removeFromCollection(collection: URI, item: URI): Unit = {

  }

  def connect(itemOne: URI, itemTwo: URI, property: URI): Unit = {

  }

  def disconnect(itemOne: URI, itemTwo: URI, property: URI): Unit = {

  }
}






/**
  * Provides caseclasses for handling relations
  */
sealed abstract class PropertyType{

case class OurFunctionalProperty() extends PropertyType
case class OurTransitiveProperty() extends PropertyType
case class OurSymmetricProperty() extends PropertyType
case class OurInverseProperty(existingProp: String) extends PropertyType
case class OurObjectProperty() extends PropertyType

}





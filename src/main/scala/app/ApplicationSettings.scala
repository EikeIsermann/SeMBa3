package app

import java.io.File

import utilities.XMLFactory

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
object ApplicationSettings {

}

object Paths {

  val configFile = XMLFactory.getConfigXMLAsElemAdv



  val libConfiguration = getString("libraryConfigurationFile")
  val libraryRootFolder = getString("libraryRootFolder")
  val resourceDefinitionURI = getString("resourceDefinitionURI")
  val itemClassURI = getString("itemClassURI")
  val collectionClassURI = getString("collectionClassURI")
  val thumbnailLocationURI = getString("thumbnailLocationURI")
  val generatedDatatypePropertyURI = getString("generatedDatatypePropertyURI")
  val customDatatypePropertyURI = getString("customDatatypePropertyURI")
  val metadataPropertyURI = getString("metadataPropertyURI")
  val sembaRelationURI = getString("sembaRelationURI")
  val sembaTitle = getString("sembaTitleURI")
  def getString(s: String): String =  configFile.getValueAt("paths", s)
}

object Presets{

  val configFile = XMLFactory.getConfigXMLAsElemAdv
  def getString(s: String): String =  configFile.getValueAt("presets", s)
  val grpcPort = getString("grpcPort")
  val validOntologyExtensions = getString("validOntologyExtensions").toLowerCase.replaceAll(" ","").split(",")
}



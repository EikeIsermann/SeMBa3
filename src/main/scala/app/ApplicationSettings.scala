package app

import utilities.XMLFactory

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
object ApplicationSettings {

}

/**
  * Global variables providing core URIs and Paths
  */
object Paths {

  private val configFile = XMLFactory.getConfigXMLAsElemAdv

  /** Name convention for a library config file  */
  val libConfiguration = getString("libraryConfigurationFile")

  /** URI of the Datatype Property linking the root folder of a library */
  val libraryRootFolder = getString("libraryRootFolder")

  /** URI of the superclass all valid SemBA resources inherit */
  val resourceDefinitionURI = getString("resourceDefinitionURI")

  /** URI of the superclass all valid SemBA items inherit */
  val itemClassURI = getString("itemClassURI")

  /** URI of the superclass all valid SemBA collections inherit */
  val collectionClassURI = getString("collectionClassURI")

  /** URI of the Datatype Property linking to the thumbnail URI of a resource */
  val thumbnailLocationURI = getString("thumbnailLocationURI")

  /** URI of the Datatype Superproperty all valid automatically generated Metadata inherit */
  val generatedDatatypePropertyURI = getString("generatedDatatypePropertyURI")

  /** URI of the Datatype Superproperty all valid custom Metadata inherit */
  val customDatatypePropertyURI = getString("customDatatypePropertyURI")

  /** URI of the Datatype Superproperty all valid Metadata inherit */
  val metadataPropertyURI = getString("metadataPropertyURI")

  /** URI of the Object Superproperty all valid Relations inherit */
  val sembaRelationURI = getString("sembaRelationURI")

  /** URI of the Data Property defining the main title of a Resource. Required for all valid Resources. */
  val sembaTitle = getString("sembaTitleURI")

  /** URI of the Object Property linking collection items to their source */
  val linksToSource = getString("mediaItemLink")

  /** URI of the Object Property linking collection items to their collection */
  val isPartOfCollection = getString("isPartOfCollectionURI")

  /** URI of the Object Property linking collections to collection items */
  val hasCollectionItem = getString("hasCollectionItemURI")

  /** URI of the superclass all valid SeMBa collection items inherit */
  val collectionItemURI = getString("collectionItemURI")

  /** Gets each config entry. */
  def getString(s: String): String = configFile.getValueAt("paths", s)
}

/** Global Presets
  *
  */
object Presets {

  private val configFile = XMLFactory.getConfigXMLAsElemAdv
  /** Port used by gRPC server instance */
  val grpcPort = getString("grpcPort")

  /** Valid file extensions for ontology files */
  val validOntologyExtensions = getString("validOntologyExtensions").toLowerCase.replaceAll(" ", "").split(",")

  /** Gets each config entry. */
  def getString(s: String): String = configFile.getValueAt("presets", s)
}



package core

import java.io.{File, FileOutputStream}
import java.net.URI

import app.Paths
import org.apache.jena.ontology.{DatatypeProperty, Individual, OntModel, OntProperty}
import org.apache.jena.ontology.impl.OntModelImpl
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.shared.Lock
import org.apache.jena.util.iterator.ExtendedIterator
import org.apache.jena.vocabulary.{OWL, OWL2, RDF, RDFS}
import sembaGRPC.{Library, LibraryConcepts, LibraryContent}
import utilities.{Convert, TextFactory}

import scala.collection.mutable.ArrayBuffer

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */

object LibraryAccess {

  /**
   * Modification
   */
  def load( parent: OntModel, path: URI ): OntModel = {
    parent.read(path.toString)
    //TODO verify if ontology contains semba-main concepts & file locations
    parent
  }

  def writeModel( model: OntModel, path: URI) = {
    model.enterCriticalSection(Lock.WRITE)
    try{
     model.write(new FileOutputStream(new File(path)),"TURTLE")
    }
    finally model.leaveCriticalSection()

  }

  def addToLib( parent: OntModel, child: ArrayBuffer[OntModel]): OntModel = {
    parent.enterCriticalSection(Lock.WRITE)
    try {
      child.foreach(parent.addSubModel(_))
    }
    finally parent.leaveCriticalSection()
    parent
  }

  def save( lib: OntModel, itemID: String ): Unit ={
    val item =  lib.getImportedModel(itemID)

  }

  def getClone(lib: OntModel): OntModel = {
    val time = System.currentTimeMillis()
    val clone = new OntModelImpl(lib.getSpecification, ModelFactory.createModelForGraph(lib.getGraph))
    println("Cloning took" + (System.currentTimeMillis() - time))
    clone
  }

  def generateDatatypeProperty(key: String, model: OntModel, baseURI: String, functional: Boolean = false): DatatypeProperty = {
    model.enterCriticalSection(Lock.WRITE)
    val cleanKey = TextFactory.cleanString(key)
    var prop = None: Option[DatatypeProperty]
    try {
       prop = Some(model.createDatatypeProperty(baseURI + cleanKey, functional))
       prop.get.addSuperProperty(model.getDatatypeProperty(Paths.generatedDatatypePropertyURI))
       prop.get.addDomain(model.getResource(Paths.resourceDefinitionURI))
       //if(functional) prop.get.addProperty(RDF.`type`, OWL.FunctionalProperty)
    }
    finally model.leaveCriticalSection()
    prop.get
  }

  def setDatatypeProperty( uri: String, model: OntModel, item: Individual, values: Array[String]): Individual = {
    var prop: Option[DatatypeProperty] = None
    model.enterCriticalSection(Lock.WRITE)

    try{
       prop = Some(model.getDatatypeProperty(uri))
    }

    finally model.leaveCriticalSection()
    if(prop.isDefined) setDatatypeProperty(prop.get,item.getOntModel,item,values)
    item
  }

  def setDatatypeProperty(
                           prop: DatatypeProperty, model: OntModel, item: Individual, values: Array[String]
                         ): Individual = {
    val functional = {
      prop.getOntModel.enterCriticalSection(Lock.WRITE)
      try {
        prop.isFunctionalProperty
      }
      finally prop.getOntModel.leaveCriticalSection()
    }

    model.enterCriticalSection(Lock.WRITE)
    try{

      if(functional){
      require(values.length == 1)
      item.removeAll(prop)
      item.addProperty(prop, values.head)

      }

      else
    {
      values.foreach( x => item.addProperty(prop, x))
    }
    }

    finally model.leaveCriticalSection()
    item
  }



  /**
    * Retrieval
    */

  def retrieveLibConcepts(model: OntModel): LibraryConcepts ={
    var retVal = new LibraryConcepts()
    model.enterCriticalSection(Lock.READ)
    try {
      val metadataProperties =
        Option(model.getDatatypeProperty(Paths.metadataPropertyURI))
     if(metadataProperties.isDefined) {
       val iter = metadataProperties.get.listSubProperties()
      while(iter.hasNext){
        val prop = iter.next()
        val annotation = Convert.ann2grpc(prop.asDatatypeProperty())
        retVal = retVal.addAnnotations((prop.getLocalName,annotation))
      }
     }
      val relationProperties =
        Option(model.getObjectProperty(Paths.sembaRelationURI))
      if(relationProperties.isDefined){
        val iter = relationProperties.get.listSubProperties()
      while(iter.hasNext){
        val prop = iter.next()
        val relation = Convert.rel2grpc(prop.asObjectProperty())
        retVal = retVal.addRelations((prop.getLocalName,relation))
      }
      }
    }
    finally model.leaveCriticalSection()

    retVal
  }

  def retrieveLibContent(model: OntModel, lib: Library): LibraryContent = {
     var retVal = LibraryContent()
    model.enterCriticalSection(Lock.READ)
    try{
      var individuals =  Option(model.listIndividuals(model.getOntClass(Paths.resourceDefinitionURI)))
      if(individuals.isDefined){
        var iter = individuals.get
      while(iter.hasNext){
        val ind = iter.next()
        val uri = ind.getURI
        retVal = retVal.addLibContent((uri, Convert.item2grpc(lib, ind)))
      }
    }      }
    finally model.leaveCriticalSection()
     retVal
  }


}

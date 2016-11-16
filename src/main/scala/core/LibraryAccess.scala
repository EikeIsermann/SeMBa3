package core

import java.io.{File, FileOutputStream}
import java.net.URI

import app.Paths
import org.apache.jena.ontology.impl.OntModelImpl
import org.apache.jena.ontology.{DatatypeProperty, Individual, OntModel}
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.shared.Lock
import sembaGRPC._
import utilities.{Convert, TextFactory}

import scala.collection.mutable.ArrayBuffer

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
        //TODO static access to used properties etc. instead of Paths.XXX
object LibraryAccess {

  /**
    * Modification
    */
  def load(parent: OntModel, path: URI): OntModel = {
    parent.read(path.toString)
    //TODO verify if ontology contains semba-main concepts & file locations
    parent
  }

  def writeModel(model: OntModel): Unit =
  {
    model.enterCriticalSection(Lock.WRITE)
    try {
      model.write(new FileOutputStream(new File(model.listOntologies().next.getURI)), "TURTLE")
    }
    finally model.leaveCriticalSection()
  }

  def writeModel(model: OntModel, path: URI) = {
    model.enterCriticalSection(Lock.WRITE)
    try {
      model.write(new FileOutputStream(new File(path)), "TURTLE")
    }
    finally model.leaveCriticalSection()

  }

  def addToLib(parent: OntModel, child: ArrayBuffer[OntModel]): OntModel = {
    parent.enterCriticalSection(Lock.WRITE)
    try {
      child.foreach(model => {parent.addSubModel(model)})
    }
    finally parent.leaveCriticalSection()
    parent
  }

  def save(lib: OntModel, itemID: String): Unit = {
    val item = lib.getImportedModel(itemID)

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

  def setDatatypeProperty(uri: String, model: OntModel, item: Individual, values: Array[String]): Individual = {
    var prop: Option[DatatypeProperty] = None
    model.enterCriticalSection(Lock.WRITE)

    try {
      prop = Some(model.getDatatypeProperty(uri))
    }

    finally model.leaveCriticalSection()
    if (prop.isDefined) setDatatypeProperty(prop.get, item.getOntModel, item, values)
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
    try {

      if (functional) {
        require(values.length == 1)
        item.removeAll(prop)
        item.addProperty(prop, values.head)

      }

      else {
        values.foreach(x => item.addProperty(prop, x))
      }
    }

    finally model.leaveCriticalSection()
    item
  }


  /**
    * Retrieval
    */

  def retrieveLibConcepts(model: OntModel): LibraryConcepts = {
    var retVal = new LibraryConcepts()
    model.enterCriticalSection(Lock.READ)
    try {
      val metadataProperties =
        Option(model.getDatatypeProperty(Paths.metadataPropertyURI))
      if (metadataProperties.isDefined) {
        val iter = metadataProperties.get.listSubProperties()
        while (iter.hasNext) {
          val prop = iter.next()
          val annotation = Convert.ann2grpc(prop.asDatatypeProperty())
          retVal = retVal.addAnnotations((prop.getLocalName, annotation))
        }
      }
      val relationProperties =
        Option(model.getObjectProperty(Paths.sembaRelationURI))
      if (relationProperties.isDefined) {
        val iter = relationProperties.get.listSubProperties()
        while (iter.hasNext) {
          val prop = iter.next()
          val relation = Convert.rel2grpc(prop.asObjectProperty())
          retVal = retVal.addRelations((prop.getLocalName, relation))
        }
      }
    }
    finally model.leaveCriticalSection()

    retVal
  }

  def retrieveLibContent(model: OntModel, lib: Library): LibraryContent = {
    var retVal = LibraryContent()
    model.enterCriticalSection(Lock.READ)
    try {
      var individuals = Option(model.listIndividuals(model.getOntClass(Paths.resourceDefinitionURI)))
      if (individuals.isDefined) {
        var iter = individuals.get
        while (iter.hasNext) {
          val ind = iter.next()
          val uri = ind.getURI
          retVal = retVal.addLibContent((uri, Convert.item2grpc(lib, ind)))
        }
      }
    }
    finally model.leaveCriticalSection()
    retVal
  }

  def retrieveMetadata(item: String, model: OntModel): ItemDescription = {
    var retVal = ItemDescription().withItemURI(item)
    model.enterCriticalSection(Lock.READ)
    try {
      val indOption = Option(model.getIndividual(item))
      val superPropOption = Option(model.getDatatypeProperty(Paths.metadataPropertyURI))
      if(superPropOption.isDefined && indOption.isDefined){
        val ind = indOption.get
        val superProp = superPropOption.get
        val iter = ind.listProperties()
        while (iter.hasNext){
          val stmt = iter.next()
          val uri =  stmt.getPredicate.getURI
          val dataProp = Option(model.getDatatypeProperty(uri))
          if( dataProp.isDefined && dataProp.get.hasSuperProperty(superProp, false))
          {
          val valueList = AnnotationValue().addValue(stmt.getObject.asLiteral().getString)
          retVal = retVal.addMetadata((stmt.getPredicate.getURI, valueList))
        }

        }
      }
    }
    finally model.leaveCriticalSection()
    retVal
  }

  def removeIndividual(item: String, model: OntModel): Unit = {
    model.enterCriticalSection(Lock.READ)
    try {
      val ind = model.getIndividual(item)
      val iter = model.listSubjectsWithProperty(model.getProperty(Paths.linksToSource), item)
      while(iter.hasNext){
        model.getOntResource(iter.nextResource()).remove()
      }
      ind.remove()

    }
    model.leaveCriticalSection()
  }

}

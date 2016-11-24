package core

import java.io.{File, FileOutputStream}
import java.net.URI
import java.util.UUID

import akka.agent.Agent
import app.Paths
import org.apache.jena.ontology.impl.OntModelImpl
import org.apache.jena.ontology._
import org.apache.jena.rdf.model.{Model, ModelFactory, RDFNode}
import org.apache.jena.shared.Lock
import sembaGRPC._
import utilities.{Convert, TextFactory, UpdateMessageFactory}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.immutable.HashMap
import scala.collection.mutable.ArrayBuffer

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
        //TODO static access to used properties etc. instead of Paths.XXX, Selector for Queries

object LibraryAccess {


  val activeModels = Agent[HashMap[String, OntModel]](HashMap[String, OntModel]())

  /**
    * Modification
    */

  def load(parent: OntModel, path: String): OntModel = {
    parent.read(path, null, "TTL")
    //TODO verify if ontology contains semba-main concepts & file locations
    parent
  }

  def writeModel(model: OntModel): Option[String] =
  {
    var uri: Option[String] = None
    model.enterCriticalSection(Lock.WRITE)
    try {
      val loc = model.listOntologies().next.getURI
      model.write(new FileOutputStream(new File(new URI(loc))), "TURTLE")
      uri = Some(loc)
    }
    finally model.leaveCriticalSection()
    uri
  }

  def writeModel(model: OntModel, path: URI) = {
    model.enterCriticalSection(Lock.WRITE)
    try {
      model.write(new FileOutputStream(new File(path)),"TURTLE")
    }
    finally model.leaveCriticalSection()

  }

  def getModelForItem(item: String): OntModel = {
    activeModels().get(item) match
      {
      case Some(model: OntModel) => model
      case None => throw new NoSuchElementException("No Model available for item: " + item)
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

  def removeFromLib(child: OntModel, parent: OntModel) = {
    parent.enterCriticalSection(Lock.WRITE)
    try{
       parent.removeSubModel(child, true)
    }
    finally parent.leaveCriticalSection()
  }


  def getClone(lib: OntModel): OntModel = {
    val time = System.currentTimeMillis()
    val clone = new OntModelImpl(lib.getSpecification, ModelFactory.createModelForGraph(lib.getGraph))
    println("Cloning took" + (System.currentTimeMillis() - time))
    clone
  }

  def generateDatatypeProperty(key: String, model: OntModel, functional: Boolean = false): Option[DatatypeProperty] = {
    model.enterCriticalSection(Lock.WRITE)
    var prop = None: Option[DatatypeProperty]

      try {
        prop = Option(model.getDatatypeProperty(key))
        if( prop.isEmpty ) {
        prop = Some(model.createDatatypeProperty(key, functional))
        prop.get.addSuperProperty(model.getDatatypeProperty(Paths.generatedDatatypePropertyURI))
        prop.get.addDomain(model.getResource(Paths.resourceDefinitionURI))
        //if(functional) prop.get.addProperty(RDF.`type`, OWL.FunctionalProperty)
         }
        else prop = None

      }
    finally model.leaveCriticalSection()
    prop
  }

  def setDatatypeProperty(uri: String, model: OntModel, item: Individual, values: Array[String]): ArrayBuffer[AnnotationValue] = {
    var prop: Option[DatatypeProperty] = None
    model.enterCriticalSection(Lock.WRITE)
    var retVal = ArrayBuffer[AnnotationValue]()
    try {
      prop = Some(model.getDatatypeProperty(uri))
    }

    finally model.leaveCriticalSection()
    if (prop.isDefined) {
       retVal = setDatatypeProperty(prop.get, item.getOntModel, item, values)
    }
    retVal
  }

  def setDatatypeProperty(
                           prop: DatatypeProperty, model: OntModel, item: Individual, values: Array[String]
                         ): ArrayBuffer[AnnotationValue] = {
    val functional = false /*{
      model.enterCriticalSection(Lock.READ)
      try {
        prop.isFunctionalProperty
      }
      finally model.leaveCriticalSection()
    }           */


    model.enterCriticalSection(Lock.WRITE)
    try {

      if (functional) {
        require(values.length == 1)
        item.removeAll(prop)
        item.addProperty(prop, values.head)
      }

      else {
        values.foreach(x => {
          item.addProperty(prop, model.createLiteral(x))
        })
      }
    }

    finally model.leaveCriticalSection()
    ArrayBuffer[AnnotationValue](AnnotationValue().addAllValue(values))
  }

  def removeDatatypeProperty(uri: String, model: OntModel, item: Individual, values: Array[String]): ArrayBuffer[AnnotationValue] =
  {
    model.enterCriticalSection(Lock.WRITE)
    try{
      val p =  model.getProperty(uri)
      for( value <- values){
        val o = model.createLiteral(value)
        model.remove(item, p, o)
      }
  //TODO!
    }
    finally model.leaveCriticalSection()
    ArrayBuffer[AnnotationValue](AnnotationValue().addAllValue(values))
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
    var indUris = ArrayBuffer[String]()
    model.enterCriticalSection(Lock.READ)
    try {
      val ontClass = model.getOntClass(Paths.resourceDefinitionURI)
      var individuals = Option(model.listIndividuals(ontClass))
      if (individuals.isDefined) {
        var iter = individuals.get
        while (iter.hasNext) {
          indUris += iter.next().getURI
        }
      }
    }
    finally model.leaveCriticalSection()
    indUris.foreach(uri => retVal = retVal.addLibContent((uri, Convert.item2grpc(lib, uri, model))))
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

  def removeIndividual(item: String, model: OntModel) = {
    model.enterCriticalSection(Lock.WRITE)
    try {
      val ind = model.getIndividual(item)
      ind.remove()
    }
    finally model.leaveCriticalSection()
  }
  //TODO test for inverse property?
  def getCollectionItems(item: String, model: OntModel): scala.collection.mutable.HashMap[String, String] = {
    val retVal = scala.collection.mutable.HashMap[String,String]()
    model.enterCriticalSection(Lock.READ)
    try {
      val linksToSource = model.getProperty(Paths.linksToSource)
      val individual = model.getIndividual(item)
      val results = model.listSubjectsWithProperty(linksToSource, individual)
      while (results.hasNext) {
        val collItem = results.next()
        val isPartOfCollection = model.getProperty(Paths.isPartOfCollection)
        val collections = model.listObjectsOfProperty(collItem, isPartOfCollection)
        while (collections.hasNext){
          retVal.put(collections.next().asResource().getURI, collItem.getURI)
        }
      }
    }
    finally model.leaveCriticalSection()
    retVal
  }



  def createRelation(origin: String, destination: String, relation: String, model: OntModel ): Unit ={
    model.enterCriticalSection(Lock.WRITE)
    try {
      val ind = model.getIndividual(origin)
      val prop = model.getProperty(relation)
      ind.addProperty(prop, destination)
    }
    finally model.leaveCriticalSection()

  }

  def removeRelation(origin: String, destination: String, relation: String, model: OntModel ): Unit = {
    model.enterCriticalSection(Lock.WRITE)
    try{
      val ind = model.getIndividual(origin)
      val prop = model.getProperty(relation)
      val destInd = model.getIndividual(destination)
      ind.removeProperty(prop, destInd)
    }
    finally model.leaveCriticalSection()
  }

  def updateMetadata(dataSet: Map[String, Array[String]], item: String, model: OntModel, delete: Boolean ): Unit ={
    var ind : Option[Individual] = None
    model.enterCriticalSection(Lock.READ)
    try {
       ind = Option(model.getIndividual(item))
    }
    finally model.leaveCriticalSection()

    if (ind.isDefined){
    for((prop, values) <- dataSet)
      {
        if (delete) setDatatypeProperty(prop, model, ind.get, values)
        else removeDatatypeProperty(prop, model, ind.get, values)
      }
    }
  }

  def addCollectionItem(collection: String, item: String, model: OntModel) = {
    //TODO import item ontmodel in collection?
    model.enterCriticalSection(Lock.WRITE)
    try{
       val newUri = Convert.uri2ont(collection) + UUID.randomUUID()
       val collItem = model.createIndividual(newUri, model.getOntClass(Paths.collectionItemURI))
       val coll = model.getIndividual(collection)
       val itemModel = getModelForItem(item)
       val itemIndividual = itemModel.getIndividual(item)
       val hasMediaItem = model.getObjectProperty(Paths.hasMediaItem)
       val hasCollectionItem = model.getObjectProperty(Paths.hasCollectionItem)
       val isPart = model.getObjectProperty(Paths.isPartOfCollection)


       collItem.setPropertyValue(model.getProperty(Paths.linksToSource),itemIndividual )
       collItem.setPropertyValue(isPart, coll)
        coll.setPropertyValue(hasCollectionItem, collItem)
       if(!model.contains(coll, hasMediaItem,itemIndividual)) coll.setPropertyValue(hasMediaItem, itemIndividual)
      //TODO figure out URI and CI format
    }
   finally model.leaveCriticalSection()
  }

  def retrieveSimpleSearchStatements(): Unit ={

  }

  def getDeepCopy(ontModel: OntModel): OntModel = {
    var retVal: Model = ontModel
    ontModel.enterCriticalSection(Lock.READ)
    try{
      retVal = ontModel.difference(ModelFactory.createDefaultModel())
    }
    finally ontModel.leaveCriticalSection()
    ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF, retVal)
  }







}

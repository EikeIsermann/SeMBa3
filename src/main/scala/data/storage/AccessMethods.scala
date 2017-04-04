package data.storage

import java.io.{File, FileOutputStream}
import java.net.URI
import java.util.UUID

import globalConstants.SembaPaths
import logic.core.LibInfo
import org.apache.jena.ontology._
import org.apache.jena.ontology.impl.OntModelImpl
import org.apache.jena.rdf.model.{Model, ModelFactory, ResourceFactory}
import org.apache.jena.shared.Lock
import org.apache.jena.util.FileUtils
import sembaGRPC._
import utilities.debug.DC
import utilities.{Convert, FileFactory, TextFactory, UpdateMessageFactory}

import scala.collection.mutable.ArrayBuffer

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
        //TODO static access to used properties etc. instead of SembaPaths.XXX, Selector for Queries

object AccessMethods {


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
      val loc = model.listOntologies().next.getURI
      model.write(new FileOutputStream(new File(new URI(loc))), "TURTLE")
      uri = Some(loc)
    uri
  }

  def writeModel(model: OntModel, path: URI) = {
      model.write(new FileOutputStream(new File(path)),"TURTLE")
  }

  /*
  def getModelForItem(item: String): OntModel = {
    activeModels().get(item) match
      {
      case Some(model: OntModel) => model
      case None => throw new NoSuchElementException("No Model available for item: " + item)
    }
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
  */
  def generateDatatypeProperty(rawKey: String, model: OntModel, config: LibInfo, functional: Boolean = false): Option[DatatypeProperty] = {
    var prop = None: Option[DatatypeProperty]
        val key = config.constants.resourceBaseURI + TextFactory.cleanString(rawKey)
        prop = Option(model.getDatatypeProperty(key))
        if( prop.isEmpty ) {
        val property = model.createDatatypeProperty(key, functional)
        property.addSuperProperty(model.getDatatypeProperty(SembaPaths.generatedDatatypePropertyURI))
        property.addDomain(model.getResource(SembaPaths.resourceDefinitionURI))
        property.addLabel(ResourceFactory.createLangLiteral(rawKey, config.constants.language))
        prop = Some(property)
        //if(functional) prop.get.addProperty(RDF.`type`, OWL.FunctionalProperty)
         }
        else prop = None
    prop
  }

  def setDatatypeProperty(uri: String, itemURI: String, model: OntModel, values: Array[String]): ArrayBuffer[AnnotationValue] = {
    val item = model.getIndividual(itemURI)
    setDatatypeProperty(uri, item, model, values)
  }

  def setDatatypeProperty(uri: String, item: Individual, model: OntModel, values:Array[String]): ArrayBuffer[AnnotationValue] = {
    var propertyOption: Option[DatatypeProperty] = None
    var retVal = AnnotationValue()
    propertyOption = Some(model.getDatatypeProperty(uri))
    require(propertyOption.isDefined)
    val prop = propertyOption.get
    if (prop.isFunctionalProperty) {
      require(values.length == 1)
      item.removeAll(prop)
      item.addProperty(prop, values.head)
      retVal = retVal.addValue(values.head)
    }
    else
    {
      {
        values.foreach(x => {
          item.addProperty(prop, model.createLiteral(x))
          retVal = retVal.addValue(x)
        })
      }
    }

    ArrayBuffer[AnnotationValue](retVal)
  }

  def removeDatatypeProperty(uri: String, itemURI: String, model: OntModel,  values: Array[String]): ArrayBuffer[AnnotationValue] =
  {
      val item = model.getIndividual(itemURI)
      removeDatatypeProperty(uri, item, model, values)
  }
  def removeDatatypeProperty(uri: String, item: Individual, model: OntModel, values: Array[String]): ArrayBuffer[AnnotationValue] = {
    val p =  model.getProperty(uri)
    values.foreach( v =>
    {
      model.remove(item, p, model.createLiteral(v))
    }
    )
    //TODO!
    ArrayBuffer[AnnotationValue](AnnotationValue().addAllValue(values))
  }


  /**
    * Retrieval
    */

  def retrieveLibConcepts(model: OntModel, config: LibInfo): LibraryConcepts = {
    var retVal = new LibraryConcepts()
    retVal = retVal.addAllAnnotations(listSubDatatypeProperties(model, SembaPaths.metadataPropertyURI, config))
      .addAllCollectionRelations(listSubObjectProperties(model, SembaPaths.sembaCollectionRelationURI, config))
      .addAllDescriptiveRelations(listSubObjectProperties(model, SembaPaths.sembaDescriptiveRelationURI, config))
      .addAllGeneralRelations(listSubObjectProperties(model, SembaPaths.sembaGeneralRelationURI, config))
    retVal
  }

  def listSubDatatypeProperties(model: OntModel, uri: String, config: LibInfo): ArrayBuffer[(String , Annotation)] = {
    val annotations = Option(model.getDatatypeProperty(uri))
    var retVal = ArrayBuffer[(String , Annotation)]()
    if (annotations.isDefined) {
       val iter = annotations.get.listSubProperties()
        while (iter.hasNext) {
        val prop = iter.next
        val annotation = (prop.getLocalName, DatastructureMapping.wrapAnnotation(prop.asDatatypeProperty(), config))
        retVal += annotation
      }
    }
    retVal
  }

  def listSubObjectProperties(model: OntModel, uri: String, config: LibInfo): ArrayBuffer[(String, Relation)] = {
    val relations = Option(model.getObjectProperty(uri))
    var retVal = ArrayBuffer[(String , Relation)]()
    if (relations.isDefined) {
      val iter = relations.get.listSubProperties()
      while (iter.hasNext) {
        val prop = iter.next
        val relation = (prop.getLocalName, DatastructureMapping.wrapRelation(prop.asObjectProperty(), config))
        retVal += relation
      }
    }
    retVal

  }

  def retrieveLibContent(model: OntModel, config: LibInfo): LibraryContent = {
    var retVal = LibraryContent().withLib(Library(config.libURI))
      val ontClass = model.getOntClass(SembaPaths.resourceDefinitionURI)
      var individuals = Option(model.listIndividuals(ontClass))
      if (individuals.isDefined) {
        var iter = individuals.get
        while (iter.hasNext) {
        val resource = iter.next()
        retVal = retVal.addLibContent((resource.getURI, DatastructureMapping.wrapResource(resource, model, config)))
        }
      }
    retVal
  }


  def retrieveMetadata(item: String, model: OntModel): ItemDescription = {
    var retVal = ItemDescription().withItemURI(item)
      val indOption = Option(model.getIndividual(item))
      val superPropOption = Option(model.getDatatypeProperty(SembaPaths.metadataPropertyURI))
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
    retVal
  }

  def removeIndividual(item: String, model: OntModel, config: LibInfo): Resource = {
    val ind = model.getIndividual(item)
    val retVal = DatastructureMapping.wrapResource(ind, model, config)
    ind.remove()
    retVal
  }
  def removeCollectionItem(uri: String, model: OntModel, config: LibInfo): CollectionItem = {
    val individual = model.getIndividual(uri)
    removeCollectionItem(individual, model, config)
  }


  def removeCollectionItem(item: Individual, model: OntModel, config: LibInfo): CollectionItem = {


    val retVal = new CollectionItem().withLib(new Library(config.libURI)).withUri(item.getURI)
    item.remove()
    retVal
  }

  //TODO test for inverse property?
  def getCollectionItems(item: String, model: OntModel): Seq[org.apache.jena.rdf.model.Resource] = {
    val retVal = ArrayBuffer[org.apache.jena.rdf.model.Resource]()
    val linksToSource = model.getProperty(SembaPaths.linksToSource)
    val individual = model.getIndividual(item)
    val results = model.listSubjectsWithProperty(linksToSource, individual)
    while(results.hasNext){
      retVal += results.nextResource()
    }
    retVal
  }

 /* def getCollectionsForItem() = {
    val results = model.listSubjectsWithProperty(linksToSource, individual)
    while (results.hasNext) {
      val collItem = results.next()
      val isPartOfCollection = model.getProperty(SembaPaths.containedByCollectionURI)
      val collections = model.listObjectsOfProperty(collItem, isPartOfCollection)
      while (collections.hasNext){
        retVal.put(collections.next().asResource().getURI, collItem.getURI)
      }
  }
   */


  def createRelation(origin: String, destination: String, relation: String, model: OntModel ): Unit ={
      val ind = model.getIndividual(origin)
      val prop = model.getProperty(relation)
      ind.addProperty(prop, destination)
  }

  def removeRelation(origin: String, destination: String, relation: String, model: OntModel ): Unit = {
      val ind = model.getIndividual(origin)
      val prop = model.getProperty(relation)
      val destInd = model.getIndividual(destination)
      ind.removeProperty(prop, destInd)
  }

  def updateMetadata(item: String, name: String, dataSet: Map[String, AnnotationValue], model: OntModel, delete: Boolean ) = { //: ItemDescription ={
    var ind : Option[Individual] = None
    ind = Option(model.getIndividual(item))
    //var retVal = ItemDescription().withItemURI(item)


    if (ind.isDefined){
      val item = ind.get
      item.setPropertyValue(model.getProperty(SembaPaths.sembaTitle),
        model.createLiteral(name))
    for((prop, values) <- dataSet)
      {
      //  val entry = {
        if (!delete) setDatatypeProperty(prop, item, model, values.value.toArray)
        else removeDatatypeProperty(prop, item, model, values.value.toArray)
    //    }
    //    retVal = retVal.addAllMetadata(entry.map(x => (prop,x)))
      }
    }
  }

  def addCollectionItem(collection: String, item: String, model: OntModel, config: LibInfo): CollectionItem = {
       val newUri = config.libURI + UUID.randomUUID()
       val collItem = model.createIndividual(newUri, model.getOntClass(SembaPaths.collectionItemURI))
       val coll = model.getIndividual(collection)
       val itemIndividual = model.getIndividual(item)
       val hasMediaItem = model.getObjectProperty(SembaPaths.hasMediaItem)
       val hasCollectionItem = model.getObjectProperty(SembaPaths.containsItemURI)
       val isPart = model.getObjectProperty(SembaPaths.containedByCollectionURI)


       collItem.setPropertyValue(model.getProperty(SembaPaths.linksToSource),itemIndividual )
       collItem.setPropertyValue(isPart, coll)
       itemIndividual.setPropertyValue(isPart,collItem)
       coll.setPropertyValue(hasCollectionItem, collItem)
       //if(!model.contains(coll, hasMediaItem,itemIndividual)) coll.setPropertyValue(hasMediaItem, itemIndividual)
      DatastructureMapping.wrapCollectionItem(collItem, model, config)
  }

  def retrieveSimpleSearchStatements(): Unit ={

  }

  def getDeepCopy(ontModel: OntModel): OntModel = {
    var retVal: Model = ontModel
      retVal = ontModel.difference(ModelFactory.createDefaultModel())
    ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF, retVal)
  }

  def uriExists(uri: String, model: OntModel): Boolean = {

    var toCheck = ResourceFactory.createResource(uri)
    var retVal = model.containsResource(toCheck)
    retVal
  }

  def createName(baseURI: String, name: String, model: OntModel): String = {
    val sanitizedName = TextFactory.sanitizeFilename(name)
    var itemName = sanitizedName
    var counts = 1
    while(uriExists((baseURI + itemName), model )){
      itemName = sanitizedName + "-" + counts
      counts += 1
    }
    itemName
  }

  def createItem(model: OntModel, name: String, ontClass: String, fileName: String, config: LibInfo, thumb: String): Resource = {
    val start = System.currentTimeMillis()
    //val itemName = createName(config.constants.resourceBaseURI, name, model)
    val itemName = UUID.randomUUID()
    val uri = config.constants.resourceBaseURI + itemName
    val root =  config.constants.dataPath + "/" + itemName + "/"

    val item = model.createIndividual(uri, model.getOntClass(ontClass))

    item.addProperty(model.getProperty(SembaPaths.sourceLocationURI),
       root + fileName)
    item.addProperty(model.getProperty(SembaPaths.thumbnailLocationURI),
      {
        if (thumb.equals(config.constants.defaultCollectionIcon)) config.constants.defaultCollectionIcon
        else root + config.constants.thumbnail
      }
    )
    item.addProperty(model.getProperty(SembaPaths.sembaTitle),
      name)
   val retVal =  DatastructureMapping.wrapResource(item, model, config)
    DC.log("Creation took " + (System.currentTimeMillis() - start))

    retVal
  }





}

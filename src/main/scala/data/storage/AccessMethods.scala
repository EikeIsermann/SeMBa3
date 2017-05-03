package data.storage

import java.io.{File, FileOutputStream}
import java.net.URI
import java.util.UUID

import globalConstants.{SembaPaths, SembaPresets}
import logic.core.Config
import org.apache.jena.ontology._
import org.apache.jena.ontology.impl.OntModelImpl
import org.apache.jena.rdf.model.{Model, ModelFactory, RDFNode, ResourceFactory, SimpleSelector, Statement}
import org.apache.jena.shared.Lock
import org.apache.jena.util.FileUtils
import sembaGRPC._
import utilities.debug.DC
import utilities.{Convert, FileFactory, TextFactory, UpdateMessageFactory}

import scala.collection.mutable
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

  def generateMetadataType(rawKey: String, model: OntModel, config: Config, functional: Boolean = false): Option[OntClass] = {
        var prop = None: Option[OntClass]
        val key = config.constants.resourceBaseURI + SembaPresets.generatedPrefix + TextFactory.cleanString(rawKey)
        prop = Option(model.getOntClass(key))
        if( prop.isEmpty ) {
        val newType = model.createClass(key)
          newType.addSuperClass(model.getOntClass(SembaPaths.generatedMetadata))
          newType.addLabel(ResourceFactory.createLangLiteral(rawKey, config.constants.language))
        prop = Some(newType)
        //if(functional) prop.get.addProperty(RDF.`type`, OWL.FunctionalProperty)
         }
        else prop = None
    prop
  }

  def setMetadataValue(uri: String, itemURI: String, model: OntModel, values: AnnotationValue): ArrayBuffer[AnnotationValue] = {
    val item = model.getIndividual(itemURI)
    setMetadataValue(uri, item, model, values)
  }

  def setMetadataValue(uri: String, item: Individual, model: OntModel, values: AnnotationValue): ArrayBuffer[AnnotationValue] = {
    var metadataInstance: Option[Individual] = getIndividualMetadataValue(uri, item, values, model)

    var retVal = AnnotationValue()

    metadataInstance.foreach(
      { metadata =>
        /*if (metadata.isFunctionalProperty) {
          require(values.length == 1)
          item.removeAll(metadata)
          item.addProperty(metadata, values.head)
          retVal = retVal.addValue(values.head)
        }
        else */{
          {
            val hasValue = model.getDatatypeProperty(SembaPaths.hasValue)
            values.value.foreach(x => {
              metadata.addProperty(hasValue, model.createLiteral(x))
              retVal = retVal.addValue(x)
            })
          }
        }
      }
    )
    ArrayBuffer[AnnotationValue](retVal)
  }

  def getIndividualMetadataValue(metadataType: String, item: Individual, annotation: AnnotationValue, model: OntModel): Option[Individual] = {
    var retVal: Option[Individual] = None
  /*
    if(Option(model.getOntClass(metadataType)).isDefined) {
        val iter = model.listStatements(
          new SimpleSelector(item, model.getProperty(SembaPaths.hasMetadata), null.asInstanceOf[RDFNode]) {
            override def selects(s: Statement): Boolean = {
              model.getIndividual(s.getObject.asResource.getURI).hasOntClass(metadataType)
            }
          }
        )
      retVal = if (iter.hasNext) Some(model.getIndividual(iter.next().getObject.asResource().getURI))
      else {*/
        retVal = Option(model.getIndividual(annotation.uri))

        if(retVal.isEmpty){
          var individual = model.createIndividual(UUID.randomUUID().toString, model.getOntClass(metadataType))
        individual.addProperty(model.getDatatypeProperty(SembaPaths.isMetadataType), metadataType)
        item.addProperty(model.getProperty(SembaPaths.hasMetadata), individual)
        retVal = Some(individual)
        }
    retVal
  }

  def updateMetadata(item: String, dataSet: Map[String, AnnotationValue], model: OntModel, delete: Boolean ) = {
    var ind : Option[Individual] = None
    ind = Option(model.getIndividual(item))

    for((prop, values) <- dataSet)
    {
      if (!delete) setMetadataValue(prop, item, model, values)
      else removeMetadataValue(prop, item, model, values)
    }
  }

  def removeMetadataValue(uri: String, itemURI: String, model: OntModel, values: AnnotationValue): ArrayBuffer[AnnotationValue] =
  {
      val item = model.getIndividual(itemURI)
      removeMetadataValue(uri, item, model, values)
  }
  def removeMetadataValue(uri: String, item: Individual, model: OntModel, values: AnnotationValue): ArrayBuffer[AnnotationValue] = {
    var metadataInstance: Option[Individual] = getIndividualMetadataValue(uri, item, values, model)
    var retVal = AnnotationValue()

    metadataInstance.foreach(
      { metadata =>
        /*if (metadata.isFunctionalProperty) {
          require(values.length == 1)
          item.removeAll(metadata)
          item.addProperty(metadata, values.head)
          retVal = retVal.addValue(values.head)
        }
        else */{
        {
          val hasValue = model.getDatatypeProperty(SembaPaths.hasValue)
          values.value.foreach(x => {
            model.remove(metadata, hasValue, model.createLiteral(x))
            retVal = retVal.addValue(x)
          })
        }
      }
      }
    )
    ArrayBuffer[AnnotationValue](retVal)
  }


  /**
    * Retrieval
    */

  def retrieveLibConcepts(model: OntModel, config: Config): LibraryConcepts = {
    var retVal = new LibraryConcepts()
    retVal = retVal.addAllAnnotations(listAnnotations(model, SembaPaths.metadataType, config))
      .addAllCollectionRelations(listSubObjectProperties(model, SembaPaths.sembaCollectionRelationURI, config))
      .addAllDescriptiveRelations(listSubObjectProperties(model, SembaPaths.sembaDescriptiveRelationURI, config))
      .addAllGeneralRelations(listSubObjectProperties(model, SembaPaths.sembaGeneralRelationURI, config))
      .addAllCollectionClasses(listSubClasses(model, SembaPaths.collectionClassURI, config))
      .addAllItemClasses(listSubClasses(model, SembaPaths.itemClassURI, config))
    retVal
  }

  def listSubClasses(model: OntModel, uri: String, config: Config): ArrayBuffer[(String, SembaClass)] = {
    val clsOption = Option(model.getOntClass(uri))
    var retVal = ArrayBuffer[(String, SembaClass)]()
    if(clsOption.isDefined){
      val cls = clsOption.get
      retVal.+=((cls.getURI, DatastructureMapping.wrapClass(cls, config)))
      val iter = cls.listSubClasses()
      while(iter.hasNext){
        val subCls = iter.next
        val sembaClass = (subCls.getURI, DatastructureMapping.wrapClass(subCls, config))
        retVal += sembaClass
      }
    }
    retVal
  }

  def listAnnotations(model: OntModel, uri: String, config: Config): ArrayBuffer[(String , Annotation)] = {
    val annotations = Option(model.getOntClass(uri))
    var retVal = ArrayBuffer[(String , Annotation)]()
    if (annotations.isDefined) {
       val iter = annotations.get.listSubClasses()
        while (iter.hasNext) {
        val prop = iter.next
        val annotation = (prop.getURI, DatastructureMapping.wrapAnnotation(prop.asClass(), config))
        retVal += annotation
      }
    }
    retVal
  }

  def listSubObjectProperties(model: OntModel, uri: String, config: Config): ArrayBuffer[(String, Relation)] = {
    val relations = Option(model.getObjectProperty(uri))
    var retVal = ArrayBuffer[(String , Relation)]()
    if (relations.isDefined) {
      val iter = relations.get.listSubProperties()
      while (iter.hasNext) {
        val prop = iter.next
        val relation = (prop.getURI, DatastructureMapping.wrapRelation(prop.asObjectProperty(), config))
        retVal += relation
      }
    }
    retVal

  }

  def retrieveLibContent(model: OntModel, config: Config): LibraryContent = {
    // Initialze return Value - Empty library content
    var retVal = LibraryContent().withLib(Library(config.libURI))

    // Retrieve all Individuals that are Resources: Items / Collections
      val ontClass = model.getOntClass(SembaPaths.resourceDefinitionURI)
      var individuals = Option(model.listIndividuals(ontClass))
      if (individuals.isDefined) {
        var iter = individuals.get

        //Convert Individuals to External Representation and add to LibraryContent
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
      val hasMetadataOption = Option(model.getObjectProperty(SembaPaths.hasMetadata))
      if(hasMetadataOption.isDefined && indOption.isDefined){
        val ind = indOption.get
        retVal = retVal.withName(ind.getPropertyValue(model.getDatatypeProperty(SembaPaths.sembaTitle)).asLiteral().toString)
        val hasMetadata = hasMetadataOption.get
        val iter = ind.listPropertyValues(hasMetadata)
        while (iter.hasNext){
          val stmt = iter.next()
          val metadataValue =  stmt.asResource()
          val propertyIter = metadataValue.listProperties(model.getDatatypeProperty(SembaPaths.hasValue))
          val values = ArrayBuffer.empty[String]
          while(propertyIter.hasNext) {
            values += propertyIter.next.getObject.asLiteral().getString

          }
          val key = metadataValue.getProperty(model.getDatatypeProperty(SembaPaths.isMetadataType)).getObject.asLiteral().toString
          val valueList = AnnotationValue().addAllValue(values).withUri(metadataValue.getURI)
          retVal = retVal.addMetadata((key, valueList))
        }

        }
    retVal
      }


  def removeIndividual(item: String, model: OntModel, config: Config): Resource = {
    val ind = model.getIndividual(item)
    val retVal = DatastructureMapping.wrapResource(ind, model, config)
    ind.remove()
    retVal
  }

  def removeCollectionItem(uri: String, model: OntModel, config: Config): CollectionItem = {
    val individual = model.getIndividual(uri)
    removeCollectionItem(individual, model, config)
  }


  def removeCollectionItem(item: org.apache.jena.rdf.model.Resource, model: OntModel, config: Config): CollectionItem = {
    val itemUri = item.getURI
    val containedBy = model.getProperty(SembaPaths.containedByCollectionURI)
    val itemParentResource = item.getPropertyResourceValue(containedBy)
    val itemParent = itemParentResource.asResource.getURI
    val retVal = new CollectionItem( uri = itemUri, parentCollection = itemParent).withLib(new Library(config.libURI))
    model.removeAll( item, null, null)
    model.removeAll(null, null, item)
    retVal
  }

  //TODO test for inverse property?
  def getCollectionItems(item: String, model: OntModel): Seq[org.apache.jena.rdf.model.Resource] = {
    val retVal = ArrayBuffer[org.apache.jena.rdf.model.Resource]()
    val linksToSource = model.getProperty(SembaPaths.linksToSource)
    val individual = model.getIndividual(item)
    val results = model.listSubjectsWithProperty(linksToSource, individual)
    while(results.hasNext){
      retVal += results.nextResource
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


  def createCollectionRelation(origin: String, destination: String, relation: String, model: OntModel, config: Config ): CollectionItem ={
      val ind = model.getIndividual(origin)
      val prop = model.getObjectProperty(relation)
      val dest = model.getIndividual(destination)
      ind.addProperty(prop, dest)
      DatastructureMapping.wrapCollectionItem(ind, model, config)
  }

  def removeCollectionRelation(origin: String, destination: String, relation: String, model: OntModel, config: Config ): CollectionItem = {
      val ind = model.getIndividual(origin)
      val prop = model.getProperty(relation)
      val destInd = model.getIndividual(destination)
      ind.removeProperty(prop, destInd)
      DatastructureMapping.wrapCollectionItem(ind, model, config)
  }

  def setName(item: String, name: String, model: OntModel, config: Config): Resource = {
    var ind = model.getIndividual(item)
      ind.setPropertyValue(model.getProperty(SembaPaths.sembaTitle),
        model.createLiteral(name))

    DatastructureMapping.wrapResource(ind, model, config)
  }



  def addCollectionItem(collection: String, item: String, model: OntModel, config: Config): CollectionItem = {
       val newUri = config.libURI + UUID.randomUUID()
       val collItem = model.createIndividual(newUri, model.getOntClass(SembaPaths.collectionItemURI))
       val coll = model.getIndividual(collection)
       val itemIndividual = model.getIndividual(item)
       val isPart = model.getObjectProperty(SembaPaths.containedByCollectionURI)


       collItem.setPropertyValue(model.getProperty(SembaPaths.linksToSource), itemIndividual )
       collItem.setPropertyValue(isPart, coll)
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

  def createItem(model: OntModel, name: String, ontClass: String, fileName: String, config: Config, thumb: String, id: UUID): Resource = {
    //val itemName = createName(config.constants.resourceBaseURI, name, model)
    val itemName = id
    val uri = config.constants.resourceBaseURI + itemName
    val root =  config.constants.dataPath + "/" + itemName + "/"

    val item = model.createIndividual(uri, model.getOntClass(ontClass))
    if(item.hasOntClass(SembaPaths.itemClassURI)) item.addProperty(model.getProperty(SembaPaths.sourceLocationURI),
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

    retVal
  }

  def retrieveCollectionContent(model: OntModel, uri: String, config: Config): CollectionContent = {
    val iter = model.listSubjectsWithProperty(model.getProperty(SembaPaths.containedByCollectionURI), model.getIndividual(uri))
    val cItems = ArrayBuffer.empty[(String, CollectionItem)]
    while (iter.hasNext)
    {
      val cItem = DatastructureMapping.wrapCollectionItem(model.getIndividual(iter.next.getURI), model, config)
      cItems.+=((cItem.uri , cItem))
    }
    CollectionContent().withUri(uri).addAllContents(cItems)
  }





}

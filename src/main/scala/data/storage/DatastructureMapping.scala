package data.storage

import globalConstants.SembaPaths
import logic.core.Config
import org.apache.jena.ontology._
import sembaGRPC._

import scala.collection.immutable.HashMap
import scala.collection.mutable.ArrayBuffer

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
object DatastructureMapping {

  def wrapAnnotation(cls:OntClass, config: Config): Annotation = {
      new Annotation(
        uri = cls.getURI,
        description = Option(cls.getComment(config.constants.language)).getOrElse("A Semba Property"),
        label = Option(cls.getLabel(config.constants.language)).getOrElse(cls.getLocalName)
      )
  }
  def wrapRelation(rel: ObjectProperty, config: Config): Relation = {
    new Relation(
      uri = rel.getURI,
      description = Option(rel.getComment(config.constants.language)).getOrElse("A Semba Relation"),
      label = Option(rel.getLabel(config.constants.language)).getOrElse(rel.getLocalName)
    )
  }

  def wrapClass(cls: OntClass, config: Config): SembaClass = {
    new SembaClass(
      uri = cls.getURI,
      description = Option(cls.getComment(config.constants.language)).getOrElse("A SembaClass"),
      label = Option(cls.getLabel(config.constants.language)).getOrElse(cls.getLocalName)
    )
  }
  def wrapResource(item: Individual, model: OntModel, config: Config): Resource = {
    require(item.hasOntClass(SembaPaths.resourceDefinitionURI))
    val isItem = item.hasOntClass(SembaPaths.itemClassURI)
    val retVal = Resource(
      Some(Library(config.libURI)), {
        if (isItem) ItemType.ITEM
        else ItemType.COLLECTION
        },
      item.getURI,
      item.getPropertyValue(model.getDatatypeProperty(SembaPaths.sembaTitle)).asLiteral().toString,
      item.getPropertyValue(model.getDatatypeProperty(SembaPaths.thumbnailLocationURI)).asLiteral().toString,
      if(isItem) item.getPropertyValue(model.getDatatypeProperty(SembaPaths.sourceLocationURI)).asLiteral().toString else ""
    )
    retVal
  }



  def wrapCollectionItem(item: Individual, model: OntModel, config: Config): CollectionItem = {
    require(item.hasOntClass(SembaPaths.collectionItemURI))
    var retVal = CollectionItem()
      .withLib(Library(config.libURI))
      .withLibraryResource(item.getPropertyValue(model.getProperty(SembaPaths.linksToSource)).asResource().getURI)
      .withParentCollection(item.getPropertyValue(model.getProperty(SembaPaths.containedByCollectionURI)).asResource().getURI)
      .withUri(item.getURI)

    val iter = item.listProperties()
    val values = ArrayBuffer[(String, String)]()
    val superProperty = model.getObjectProperty(SembaPaths.sembaCollectionRelationURI)
    while (iter.hasNext){
      val stmt = iter.next()
      val propUri = stmt.getPredicate.getURI
      val objectProperty = Option(model.getObjectProperty(propUri))
      if(objectProperty.isDefined && objectProperty.get.hasSuperProperty(superProperty, false) && objectProperty.get.getURI != superProperty.getURI)
        values.+=((stmt.getPredicate.getURI, stmt.getObject.asResource.getURI))
    }
    val valuesMap = values.foldLeft(Map.empty[String, RelationValue]) { case (acc, (k, v)) =>
      acc.updated(k, acc.getOrElse(k, RelationValue()).addDestination(v))
    }
    retVal = retVal.addAllRelations(valuesMap)
    retVal
  }










}

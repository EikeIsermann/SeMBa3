package data.storage

import globalConstants.SembaPaths
import logic.core.LibInfo
import org.apache.jena.ontology.{DatatypeProperty, Individual, ObjectProperty, OntModel}
import sembaGRPC._

import scala.collection.immutable.HashMap
import scala.collection.mutable.ArrayBuffer

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
object DatastructureMapping {

  def wrapAnnotation(prop:DatatypeProperty, config: LibInfo): Annotation = {
      new Annotation(
        uri = prop.getURI,
        description = Option(prop.getComment(config.constants.language)).getOrElse(""),
        label = Option(prop.getLabel(config.constants.language)).getOrElse(prop.getLocalName)
      )
  }
  def wrapRelation(rel: ObjectProperty, config: LibInfo): Relation = {
    new Relation(
      uri = rel.getURI,
      description = Option(rel.getComment(config.constants.language)).getOrElse(""),
      label = Option(rel.getLabel(config.constants.language)).getOrElse(rel.getLocalName)
    )
  }
  def wrapResource(item: Individual, model: OntModel, config: LibInfo): Resource = {
    require(item.hasOntClass(SembaPaths.resourceDefinitionURI))
    val retVal = Resource(
      Some(Library(config.libURI)), {
        if (item.hasOntClass(SembaPaths.itemClassURI)) ItemType.ITEM
        else ItemType.COLLECTION
        },
      item.getURI,
      item.getPropertyValue(model.getDatatypeProperty(SembaPaths.sembaTitle)).asLiteral().toString,
      item.getPropertyValue(model.getDatatypeProperty(SembaPaths.thumbnailLocationURI)).asLiteral().toString,
      item.getPropertyValue(model.getDatatypeProperty(SembaPaths.sourceLocationURI)).asLiteral().toString
    )
    retVal
  }

  def wrapCollectionItem(item: Individual, model: OntModel, config: LibInfo): CollectionItem = {
    require(item.hasOntClass(SembaPaths.collectionItemURI))
    var retVal = CollectionItem()
      .withLib(Library(config.libURI))
      .withLibraryResource(item.getPropertyValue(model.getProperty(SembaPaths.linksToSource)).asLiteral().toString)
      .withParentCollection(item.getPropertyValue(model.getProperty(SembaPaths.containedByCollectionURI)).asLiteral().toString)
      .withUri(item.getURI)

    // TODO does this work or do we need to iterate over all subproperties of CollectionRelation?
    val iter = item.listProperties(model.getProperty(SembaPaths.sembaCollectionRelationURI))
    val values = ArrayBuffer[(String, String)]()

    while (iter.hasNext){
      val stmt = iter.next()
      values.+=((stmt.getPredicate.getURI, stmt.getObject.asResource.getURI))
    }
    val valuesMap = values.foldLeft(Map.empty[String, RelationValue]) { case (acc, (k, v)) =>
      acc.updated(k, acc.getOrElse(k, RelationValue()).addDestination(v))
    }
    retVal = retVal.addAllRelations(valuesMap)
    retVal
  }










}

package data.storage

import logic.core.LibInfo
import org.apache.jena.ontology.{DatatypeProperty, ObjectProperty}
import sembaGRPC.{Annotation, Relation}

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
object DatastructureMapping {

  def wrap(prop:DatatypeProperty, config: LibInfo): Annotation = {
      new Annotation(
        uri = prop.getURI,
        description = Option(prop.getComment(config.constants.language)).getOrElse(""),
        label = Option(prop.getLabel(config.constants.language)).getOrElse("")
      )
  }
  def wrap(rel: ObjectProperty, config: LibInfo): Relation = {
    new Relation(
      uri = rel.getURI,
      description = Option(rel.getComment(config.constants.language)).getOrElse(""),
      label = Option(rel.getLabel(config.constants.language)).getOrElse("")
    )
  }






}

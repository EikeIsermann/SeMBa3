package utilities

import java.io.InputStream
import java.nio.ByteBuffer

import app.Paths
import core.Semba
import org.apache.jena.ontology.{DatatypeProperty, Individual, ObjectProperty}
import org.apache.jena.rdf.model.Property
import org.apache.jena.shared.Lock
import org.apache.jena.vocabulary.DCTerms
import sembaGRPC.LibraryConcepts.AnnotationsEntry
import sembaGRPC._
import sun.misc.{BASE64Decoder, BASE64Encoder}

import scala.util.Try

/**
 * Copyright © 2012 Christian Treffs
 * Author: Christian Treffs (ctreffs@gmail.com)
 * Date: 13.09.12 17:01
 *
 * converter main.scala.helper
 */
object Convert {

  private val base64Decoder = new BASE64Decoder
  private val base64Encode = new BASE64Encoder

  def ns2sec(nanoseconds: Long): Float = nanoseconds.toFloat/1000000000f

  def text2Name(txt: String): String = txt.trim.replaceAll(" ", "_").toLowerCase


  def base64Encode(data: Array[Byte]): String = base64Encode.encode(data)
  def base64Encode(data: ByteBuffer): String = base64Encode.encode(data)

  def base64Decode(base64: InputStream): Array[Byte] = base64Decoder.decodeBuffer(base64)
  def base64Decode(base64: String): Array[Byte] = base64Decoder.decodeBuffer(base64)
  def base64Decode2Buffer(base64: InputStream): ByteBuffer = base64Decoder.decodeBufferToByteBuffer(base64)
  def base64Decode2Buffer(base64: String): ByteBuffer = base64Decoder.decodeBufferToByteBuffer(base64)

  def uri2id(str: String):String = str.substring(str.lastIndexOf('#') + 1 )


  def lib2grpc(lib: String): Library = new Library(lib)

  def item2grpc(lib: Library, item: Individual): Resource = {
    val model = item.getOntModel
    var retVal = Resource()
    model.enterCriticalSection(Lock.READ)
    try {
      require(item.hasOntClass(Paths.resourceDefinitionURI))
      retVal = new Resource(
        Some(lib),
        item.getOntClass(true).getURI match {
          case Paths.itemClassURI => ItemType.ITEM
          case Paths.collectionClassURI => ItemType.COLLECTION
        },
        item.getURI,
        item.getPropertyValue(model.getDatatypeProperty(Paths.sembaTitle)).asLiteral().toString,
        item.getPropertyValue(model.getDatatypeProperty(Paths.thumbnailLocationURI)).asLiteral().toString
      )
    }
    finally model.leaveCriticalSection()
    retVal
  }

  def ann2grpc(prop: DatatypeProperty, lang: String = "en"): Annotation = {

     new Annotation(uri = prop.getURI,
       description = Option(prop.getComment(lang)).getOrElse(""),
       label = Option(prop.getLabel(lang)).getOrElse("")
     )
  }

  def rel2grpc(prop: ObjectProperty, lang: String = "en"): Relation = {
     new Relation(uri = prop.getURI,
       description = Option(prop.getComment(lang)).getOrElse(""),
       label = Option(prop.getLabel(lang)).getOrElse("")
     )
  }






}

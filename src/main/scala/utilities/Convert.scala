package utilities

import java.io.InputStream
import java.nio.ByteBuffer

import globalConstants.SembaPaths
import org.apache.jena.ontology.{DatatypeProperty, Individual, ObjectProperty, OntModel}
import org.apache.jena.shared.Lock
import sembaGRPC._
import sun.misc.{BASE64Decoder, BASE64Encoder}

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

  def ns2sec(nanoseconds: Long): Float = nanoseconds.toFloat / 1000000000f

  def text2Name(txt: String): String = txt.trim.replaceAll(" ", "_").toLowerCase


  def base64Encode(data: Array[Byte]): String = base64Encode.encode(data)

  def base64Encode(data: ByteBuffer): String = base64Encode.encode(data)

  def base64Decode(base64: InputStream): Array[Byte] = base64Decoder.decodeBuffer(base64)

  def base64Decode(base64: String): Array[Byte] = base64Decoder.decodeBuffer(base64)

  def base64Decode2Buffer(base64: InputStream): ByteBuffer = base64Decoder.decodeBufferToByteBuffer(base64)

  def base64Decode2Buffer(base64: String): ByteBuffer = base64Decoder.decodeBufferToByteBuffer(base64)

  def uri2id(str: String): String = str.substring(str.lastIndexOf('#') + 1)

  def uri2ont(str: String): String = str.substring(0, str.lastIndexOf('#'))

  def lib2grpc(lib: String): Library = new Library(lib)

  def item2grpc(lib: Library, itemURI: String, model: OntModel): Resource = {
    var retVal = Resource()
      val item = model.getIndividual(itemURI)
      require(item.hasOntClass(SembaPaths.resourceDefinitionURI))
      retVal = new Resource(
        Some(lib),
        item.getOntClass(true).getURI match {
          case SembaPaths.itemClassURI => ItemType.ITEM
          case SembaPaths.collectionClassURI => ItemType.COLLECTION
          case _ => ItemType.ITEM
        },
        item.getURI,
        item.getPropertyValue(model.getDatatypeProperty(SembaPaths.sembaTitle)).asLiteral().toString,
        item.getPropertyValue(model.getDatatypeProperty(SembaPaths.thumbnailLocationURI)).asLiteral().toString
      )

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

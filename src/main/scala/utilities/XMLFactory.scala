package utilities

import java.io.File
import scala.xml._


/**
 * Copyright © 2012 SeMBA Team
 * User: Chris
 * Date: 08.06.12 18:19
 */

object XMLFactory {
  private val pathToConfigFile = "appdata/ApplicationSettings.xml"
  private val configXML = FileFactory.fileToXML(pathToConfigFile)

  def getConfigXMLAsElemAdv = new ElemAdv(configXML)

  def getXMLAsElemAdv(pathToFile:String) = new ElemAdv(FileFactory.fileToXML(pathToFile))

  def getXML(pathToFile:String) = FileFactory.fileToXML(pathToFile)

  def getXML(file: File) = XML.loadFile(file)

  def getNodeValue(node: Node, key: String) = (node \\ key).head.text

  def val2XML[T](arg: T, tag: String)(implicit ev: T <:< XMLExportable): Elem = {
    if(arg != null) {XML.loadString("<"+tag+">"+{arg.toXML}+"</"+tag+">")}
    else null
  }

  def list2XML[T <: XMLExportable](list: List[T], tag: String): NodeSeq = {
    if (list != null) {list.map(e => XML.loadString("<"+tag+">"+{e.toXML}+"</"+tag+">"))}
    else {null}
  }

  def simpleList2XML(list: List[String], tag: String): NodeSeq = {
    if (list != null) {list.map(e => XML.loadString("<"+tag+">"+{TextFactory.cleanString(e)}+"</"+tag+">"))}
    else {null}
  }


}


class ElemAdv(elem: Elem) {
  val e = elem
  def getValue(key: String) = (e \\ key).head.text
  def getValueAt(key: String, key2: String) = (e \\ key \\ key2).head.text


}


trait XMLExportable {
  def toXML: Any
}
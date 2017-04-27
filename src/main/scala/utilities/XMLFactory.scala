package utilities

import java.io.{File, FileOutputStream}
import java.net.URI
import java.nio.channels.Channels
import java.nio.file.{Files, Path}

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

  def getXMLAsElemAdv(pathToFile: String) = new ElemAdv(FileFactory.fileToXML(pathToFile))

  def getXMLAsElemAdv(pathToFile: URI) = new ElemAdv(FileFactory.fileToXML(pathToFile))

  def getXML(pathToFile: String) = FileFactory.fileToXML(pathToFile)

  def getXML(file: File) = XML.loadFile(file)

  def getNodeValue(node: Node, key: String) = (node \\ key).head.text

  def val2XML[T](arg: T, tag: String)(implicit ev: T <:< XMLExportable): Elem = {
    if (arg != null) {
      XML.loadString("<" + tag + ">" + {
        arg.toXML
      } + "</" + tag + ">")
    }
    else null
  }

  def list2XML[T <: XMLExportable](list: List[T], tag: String): NodeSeq = {
    if (list != null) {
      list.map(e => XML.loadString("<" + tag + ">" + {
        e.toXML
      } + "</" + tag + ">"))
    }
    else {
      null
    }
  }

  def simpleList2XML(list: List[String], tag: String): NodeSeq = {
    if (list != null) {
      list.map(e => XML.loadString("<" + tag + ">" + {
        TextFactory.cleanString(e)
      } + "</" + tag + ">"))
    }
    else {
      null
    }
  }

  def save(node: Node, fileName: String) = {
    val Encoding = "UTF-8"
    val pp = new PrettyPrinter(80, 2)

    val src: Path = new File(URI.create(fileName)).toPath
    if(!Files.exists(src)) Files.createFile(src)

    val fos = new FileOutputStream(fileName)
    val writer = Channels.newWriter(fos.getChannel(), Encoding)

    try {
      writer.write("<?xml version='1.0' encoding='" + Encoding + "'?>\n")
      writer.write(pp.format(node))
    } finally {
      writer.close()
    }

    fileName
  }

}


class ElemAdv(elem: Elem) {
  val e = elem

  def getValue(key: String) = (e \\ key).head.text

  def getValueAt(key: String, key2: String) = (e \\ key \\ key2).head.text


}


trait XMLExportable {
  def toXML: Elem
}

/*
  def save() = saveAs(name)
  def saveAs(fileName: String) = {
    DC.logT('savingLevel,"Saving Level",name,3)
    val xml = GameEngine.entities.values.map(_.toXML)
    val pp = new PrettyPrinter(80,2)
    val fos = new FileOutputStream(GameEngine.levelsDir+"/"+fileName+".lvl")
    val writer = Channels.newWriter(fos.getChannel, LevelLoader.encoding)
    val now = Calendar.getInstance().getTime
    val sdf = new SimpleDateFormat()
    val saveTime = sdf.format(now)
    val lvl = <level name={name} saveTime={saveTime.toString}>{xml}</level>
    writer.write(pp.format(lvl))
    writer.close()
    DC.logT('savingLevel,"Level saved",name,3)
    fileName
  }ewq  
 */
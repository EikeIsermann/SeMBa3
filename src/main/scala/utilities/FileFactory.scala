package utilities

import java.io.File
import java.net.URI

import org.apache.jena.util.FileUtils
import org.apache.tika.Tika

import scala.collection.{immutable, mutable}
import scala.xml.XML


/**
  * Copyright © 2012 PacMan Team
  * User: Chris
  * Date: 08.06.12 18:17
  */

object FileFactory {

  final val TikaDefault: Tika = new Tika()

  def main(args: Array[String]) {

  }


  def contentsOfDirectory(pathToDirectory: URI, recursive: Boolean, withDirectories: Boolean, hiddenFiles: Boolean): immutable.List[File] = {
    var buffer = mutable.ListBuffer[File]()
    var dir: File = null
    def get(pathToDirectory: URI, recursive: Boolean, withDirectories: Boolean) {
      dir = new File(pathToDirectory)

      if (dir.isDirectory) {
        dir.listFiles.foreach(file => {
          if (recursive) {
            //recursive
            if (withDirectories) {
              //withDirectories
              if (file.isFile) {
                buffer += file
              }
              else {
                buffer += file
                get(file.toURI, recursive, withDirectories)
              }

            } else {
              //without Directories
              if (file.isFile) {
                buffer += file
              }
              else {
                get(file.toURI, recursive, withDirectories)
              }
            }

          } else {
            //not recursive
            if (withDirectories) {
              //withDirectories
              if (file.isFile) {
                buffer += file
              }
              else {
                buffer += file
              }
            } else {
              //without Directories
              if (file.isFile) {
                buffer += file
              }
              else {

              }
            }

          }

        }
        )
      }
      else {
        buffer += dir
      }
    }

    get(pathToDirectory, recursive, withDirectories)
    if (!hiddenFiles) {
      buffer = buffer.filter(file => !file.isHidden)
    }
    buffer.toList
  }


  def path2File(pathToFile: String): File = {
    try {
      new File(pathToFile)
    } catch {
      case e: Exception => throw new Exception(e)
    }
  }


  /**
    * determine the mime type of that file
    * takes roughly 7 seconds for roughly 16000 files
    * found mime types for 16486 files in 6129ms
    * found mime types for 16486 files in 5187ms
    *
    * @param file the File
    * @return mime type string
    */
  def getMimeTypeOf(file: File): String = {
    var ret: String = null
    try {
      ret = TikaDefault.detect(file)
    } catch {
      case e: Exception => throw new Exception(e)
      case _: Throwable =>
    }
    ret
  }

  def fileToXML(pathToFile: String) = {
    XML.loadFile(new File(pathToFile))
  }

  def fileToXML(pathToFile: URI) = {
    XML.loadFile(new File(pathToFile))
  }

  /**
    * extracts the filename from a path string (extension is not omitted)
    *
    * @param path2File
    * @return
    */
  def filePath2Name(path2File: String): String = path2File.reverse.split("/")(0).reverse

  def filterFileExtension(dir: File, valid: Array[String]): List[File] = {
    dir.listFiles().filter(_.isFile).toList.filter(file => valid.exists(file.getName.endsWith(_)))
  }

  def getURI(name: String): String = {
    var baseURI = name
    val scheme: String = FileUtils.getScheme(baseURI)
    if (scheme != null) {
      if (scheme == "file") if (!baseURI.startsWith("file:///")) try {
        // Fix up file URIs.  Yuk.
        val tmp: String = baseURI.substring("file:".length)
        val f: File = new File(tmp)
        baseURI = "file://" + f.getCanonicalPath
        baseURI = baseURI.replace('\\', '/')
        //                        baseURI = baseURI.replace(" ","%20");
        //                        baseURI = baseURI.replace("~","%7E");
        // Convert to URI.  Except that it removes ///
        // Could do that and fix up (again)
        //java.net.URL u = new java.net.URL(baseURI) ;
        //baseURI = u.toExternalForm() ;
      }
      catch {
        case ex: Exception => {
        }
      }
      return baseURI
    }

    if (baseURI.startsWith("/")) return "file://" + baseURI
    return "file:" + baseURI
  }

}
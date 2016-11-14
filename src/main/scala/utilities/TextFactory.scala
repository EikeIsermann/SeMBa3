package utilities

/**
 * Copyright © 2012 DMAMS Team
 * Help: https://trinity.informatik.uni-wuerzburg.de/redmine-stud/projects/sq2011p3/wiki/Wiki
 * User: Chris
 * Date: 30.03.12 20:56
 */

trait TextFactory {
  def cleanString(raw: String): String = TextFactory.cleanString(raw)
}

object TextFactory {
  def cleanString(raw: String): String = raw.toString.trim.replaceAll("\t", "_").replaceAll("\n", "_").replaceAll("\r", "_")

  def validURI(raw:String): String =
  {
        ""

  }

  //TODO bool add extension folder
  def omitExtension(s: String, completePath: Boolean = false): String = {
     val separator = System.getProperty("file.separator")
     var filename = s
     var lastSeparatorIndex = s.lastIndexOf(separator)
     if (lastSeparatorIndex != -1) filename = s.substring(lastSeparatorIndex + 1)

     var extensionIndex = filename.lastIndexOf(".")
     if (  extensionIndex != -1 ) filename = filename.substring(0, extensionIndex)

     if(completePath) filename =  s.substring(0, lastSeparatorIndex + 1) + filename
     filename

  }

  def createOntURI( baseURI: String, ontName: String): String = baseURI + "/" + ontName


  def sanitizeFilename(name: String): String = {
    val rc = new StringBuffer()
    for(c <- name)
    {
      var valid = c >= 'a' && c <= 'z'
      valid = valid || (c >= 'A' && c <= 'Z')
      valid = valid || (c >= '0' && c <= '9')
      valid = valid || (c == '_') || (c == '-') || (c == '.') || (c=='#')  || (c == '\\') //|| (c == '/')
      if (valid) rc.append(c)
      else {
        rc.append('-')
      }
    }
    rc.toString
  }



}

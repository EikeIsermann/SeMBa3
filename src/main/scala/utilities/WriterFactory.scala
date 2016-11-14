package utilities

import java.io.{File, FileInputStream, FileOutputStream, FileWriter}
import java.net.URI
import java.util.concurrent.ConcurrentSkipListSet

/**
  * Copyright © 2012 DMAMS Team
  * Help: https://trinity.informatik.uni-wuerzburg.de/redmine-stud/projects/sq2011p3/wiki/Wiki
  * User: Alex
  * Date: 24.03.12 16:40
  */
object WriterFactory {
  //TODO similar name concurrency issue
  var LockedFilenames = new ConcurrentSkipListSet[File]()

  def writeToFile(fileName: String, data: String) {
    def using[A <: {def close()}, B](param: A)(f: A => B) =
      try {
        f(param)
      } finally {
        param.close()
      }

    using(new FileWriter(fileName)) {
      fileWriter => fileWriter.write(data)
    }
  }

  def createFolder(root: URI, filename: String): File = {
    val name = (root + "/" + filename)

    var retVal = getFilenameAvailable(name)

    retVal.mkdirs()

    LockedFilenames.remove(retVal)
    retVal
  }

  def getFilenameAvailable(name: String): File = {
    var retVal = new File(new URI(name))
    var i = 1
    var available = LockedFilenames.add(retVal) && !retVal.exists()
    while (!available) {
      retVal = new File(new URI(name + "_" + i))
      available = !retVal.exists() && LockedFilenames.add(retVal)
      i = i + 1
    }
    retVal
  }

  def writeFile(origin: File, destination: File, copy: Boolean = true): File = {
    if (!destination.exists()) {
      destination.mkdirs()
    }
    var addedItem = new File(new URI(destination.toURI + TextFactory.sanitizeFilename(origin.getName)))
    /*if (addedItem.exists()){
      var i = 1
      while(addedItem.exists){
        addedItem =  new File(destination.getAbsolutePath +"/"+i+"_"+origin.getName)
        i += 1

      }

    }   */
    new FileOutputStream(addedItem) getChannel() transferFrom(new FileInputStream(origin).getChannel, 0, Long.MaxValue)
    if (!copy) {
      origin.delete()
    }
    addedItem
  }


}


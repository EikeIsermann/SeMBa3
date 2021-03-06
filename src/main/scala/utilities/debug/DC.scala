package utilities.debug

import java.util.UUID

import utilities.Convert

import scala.collection.mutable

/**
  * Copyright © 2012 PacMan Team
  * User: Chris
  * Date: 08.06.12 19:00
  */

/**
  * provides flexible functionality for debugging
  */
object DC {

  private val debug = true
  private val timestamps = mutable.HashMap[UUID, Timestamp]()
  /**
    * set true/false for main.scala.utilities.debug messages on/off
    */
  private var time: Long = 0

  def error(str: String) = {
    if (debug) {
      val msg = "\t[\u2620 ERROR] " + str
      println(msg)
    }
    str
  }

  def warn(str: String) = {
    if (debug) {
      val msg = "\t[\u26A0 WARNING] " + str
      println(msg)
    }
    str
  }

  def logInline(str: String) = {
    if (debug) {
      val msg = str + " "
      print(msg)
    }
    str
  }


  def log(str: String) = {
    if (debug) {
      val msg = "[LOG] " + str
      println(msg)
    }
    str
  }

  def logStart(str: String) = {
    if (debug) {
      time = System.nanoTime()
      val msg = "[LOG] " + str + "..."
      print(msg)
    }
    str
  }

  def logEnd() {
    if (debug) {
      val now = System.nanoTime()
      time = now - time
      val msg = "done (" + Convert.ns2sec(time) + " s)"
      println(msg)
      time = 0
    }
  }

  def measure(label: String): Timestamp = {
    val stamp = Timestamp(label)
    timestamps.put(stamp.id,stamp)
    stamp
  }
  def stop(stamp: Timestamp) = {
    timestamps.remove(stamp.id).foreach(x =>
      DC.log("Code Segment: " + x.label + " took " + (System.currentTimeMillis() - stamp.time) + "ms" )
    )
  }




  /*
    def logEnvironment() {
      if(debug) {
        var msg = ""
        msg = "[ENV] System: "+Properties.osName +" "+ Properties.propOrEmpty("os.version")+
          "; Java: "+Properties.javaVersion+" by "+Properties.javaVendor+
          "; Scala: "+Properties.scalaPropOrEmpty("version.number")+
          "; Encoding: "+Properties.sourceEncoding
        println(msg)
      }
    }
  */
}
case class Timestamp(label: String, id: UUID = UUID.randomUUID(), time: Long = System.currentTimeMillis())

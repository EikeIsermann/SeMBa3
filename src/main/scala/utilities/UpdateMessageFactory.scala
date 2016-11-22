package utilities

import sembaGRPC.Notification

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
object UpdateMessageFactory {

  def getNotification(success: String, fail: String, uri: Option[String]): Notification ={
    uri match {
      case Some(str) => Notification().withMsg(success).withUri(str)
      case None =>  Notification().withMsg(fail)
    }
  }
}

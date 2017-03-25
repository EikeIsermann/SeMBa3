package logic.resourceCreation

import java.io.File
import java.net.URI

import logic.core.{JobProtocol, JobReply, JobResult}
import sembaGRPC.{ItemDescription, ItemType, UpdateMessage}

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
object CreationMessages {
   case class CreateInStorage(itemType: ItemType, src: String, ontClass: String, desc: ItemDescription, thumb: URI) extends JobProtocol
}

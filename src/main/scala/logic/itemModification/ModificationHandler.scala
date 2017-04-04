package logic.itemModification

import akka.actor.{Actor, Props}
import api._
import logic.core.{JobHandling, LibInfo}

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
class ModificationHandler(config: LibInfo) extends Actor with JobHandling {
  override def wrappedReceive: Receive = {

    case removeIt: RemoveFromLibrary => {

    }
    case updateMeta: UpdateMetadata => {

    }

    case removeCollItem: RemoveCollectionItem => {

    }

    case createRel: CreateRelation => {

    }
    case removeRel: RemoveRelation => {

    }
  }

}

object ModificationHandler{

  def props(config: LibInfo) = Props(new ModificationHandler(config))
}

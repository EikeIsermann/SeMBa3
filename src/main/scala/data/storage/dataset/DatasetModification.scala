package data.storage.dataset

import core.{JobProtocol, JobReply}
import org.apache.jena.query.Dataset
import org.apache.jena.rdf.model.Model
import sembaGRPC.UpdateMessage

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */

class DatasetModification(data: Dataset, uri: String) extends StorageModification(uri)
{
  override def library: Model = ???

  override def registerOntology(regModel: RegisterOntology): UpdateMessage = ???

  override def receive: Receive = ???

  override def handleJob(jobProtocol: JobProtocol): JobReply = ???
}

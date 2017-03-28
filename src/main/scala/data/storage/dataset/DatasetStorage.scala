package data.storage.dataset

import java.io.File
import java.net.URI
import java.nio.file.{Files, Paths}

import data.storage.{AccessMethods, SembaStorage, SembaStorageComponent}
import globalConstants.SembaPaths
import logic.core.{JobHandling, JobProtocol, JobReply, LibInfo}
import org.apache.jena.ontology.{OntModel, OntModelSpec}
import org.apache.jena.query.ReadWrite
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.jena.shared.Lock
import org.apache.jena.tdb.{TDB, TDBFactory}

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
class DatasetStorage(config: LibInfo) extends SembaStorageComponent {
  val loc = new File(new URI(config.constants.storagePath)).getAbsolutePath
  val data = TDBFactory.createDataset(loc)
  data.getContext.set(TDB.symUnionDefaultGraph, true)
  val basemodel = ModelFactory.createOntologyModel()
  val path = config.constants.baseOntologyURI
  basemodel.enterCriticalSection(Lock.WRITE)
    try {
      if (!Files.exists(Paths.get(new URI(path)))) {
        basemodel.createOntology(path)
        basemodel.addLoadedImport(SembaPaths.mainUri)
        basemodel.setNsPrefix(SembaPaths.mainUri, "main")
        basemodel.setNsPrefix(path, "")
      }
      else {
        AccessMethods.load(basemodel, path)
      }
    }
    finally basemodel.leaveCriticalSection()

  data.begin(ReadWrite.WRITE)
  try{
    var model = data.getNamedModel(path)
    model = model.union(basemodel)
    val withReasoning = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, basemodel)
    data.addNamedModel(path, withReasoning)
    data.commit()
  }
  finally data.end()

  override def getModel(uri: String): Model = ???

  override def getOntModel(uri: String): OntModel = ???

  override def getUnionModel(): Model = ???

  override def getBaseModel(): Model = ???

  override def performRead(model: Model): Unit = ???

  override def performWrite(model: Model): Unit = ???

  override def endRead(model: Model): Unit = ???

  override def endWrite(model: Model): Unit = ???

  override def save(): Unit = ???

  override def load(path: URI): Unit = ???

  override def containsModel(uri: String): Boolean = ???
}

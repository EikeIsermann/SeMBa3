package data.storage.dataset

import java.io.File
import java.net.URI
import java.nio.file.{Files, Paths}

import data.storage.{AccessMethods, SembaStorage, SembaStorageComponent}
import globalConstants.SembaPaths
import logic.core._
import logic.core.jobHandling.ErrorResult
import org.apache.jena.ontology.{OntModel, OntModelSpec}
import org.apache.jena.query.ReadWrite
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.jena.reasoner.ReasonerRegistry
import org.apache.jena.shared.Lock
import org.apache.jena.tdb.{TDB, TDBFactory}
import utilities.debug.DC

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
class DatasetStorage(config: Config) extends SembaStorageComponent {
  val tBoxName = "tBox"
  val aBoxName = "aBox"
  val deductionsName = "deductions"

  val loc = new File(new URI(config.rootFolder + config.constants.storagePath)).getAbsolutePath
  val data = TDBFactory.createDataset(loc)
  data.getContext.set(TDB.symUnionDefaultGraph, true)

  override def getABox(): OntModel = {

    val schema = data.getNamedModel(tBoxName)
    //val start = System.currentTimeMillis()

    val aBoxModel =  ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_RDFS_INF,
      data.getNamedModel(aBoxName))
    aBoxModel.addSubModel(schema)
    //DC.log("Execution of getABox took " + (System.currentTimeMillis() - start ))

    aBoxModel
    //val reasoner = ReasonerRegistry.getOWLMicroReasoner.bindSchema(schema)
    //ModelFactory.createInfModel(reasoner, aBoxModel)
  }

  override def getTBox(): OntModel = {
     ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RDFS_INF,
       data.getNamedModel(tBoxName))
  }

  override def saveTBox(model: OntModel) = {
    if(data.getLock.equals(Lock.WRITE))
    data.addNamedModel(tBoxName, model)
  }

  override def saveABox(model: OntModel) = {
    if(data.getLock.equals(Lock.WRITE)){
    //val start = System.currentTimeMillis()

    data.addNamedModel(aBoxName, model.getBaseModel)
    //DC.log("Execution of saveABox took " + (System.currentTimeMillis() - start ))
    }
  }

  //TODO even if this works - look into separating deductions, having InfModels
  //TODO or plain Models in Union for read access and separating the reasoning process

  override def performRead[T](f: => T): T = {
    var retVal = None: Option[T]
    //val startLock = System.currentTimeMillis()
    data.begin(ReadWrite.READ)

    try {
      retVal = Option(f)
      data.commit()


    }
     //TODO ExceptionHandling
    finally data.end()
    //DC.log("PerformRead took " + (System.currentTimeMillis() - startLock ))

    retVal.get

  }


  override def performWrite[T](f: => T): T = {
    var retVal = None: Option[T]
    //val startLock = System.currentTimeMillis()
    data.begin(ReadWrite.WRITE)
    try {
      retVal = Option(f)
      data.commit()

    }
    finally data.end()
    //DC.log("PerformWrite took " + (System.currentTimeMillis() - startLock ))
    retVal.get

  }

  override def initialize() = {
    val tBox = ModelFactory.createOntologyModel()
    val path = config.libURI

    tBox.enterCriticalSection(Lock.WRITE)
    try {
      if (!Files.exists(Paths.get(new URI(path)))) {
        tBox.createOntology(path)
        tBox.addLoadedImport(SembaPaths.mainUri)
        tBox.setNsPrefix(SembaPaths.mainUri, "main")
        tBox.setNsPrefix(path, "")
      }
      else {
        AccessMethods.load(tBox, path)
      }
    }
    finally tBox.leaveCriticalSection()

    data.begin(ReadWrite.WRITE)
    try{
      var model = Option(data.getNamedModel(tBoxName))
      var unionModel = model.getOrElse(tBox).union(tBox)
      val withReasoning = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, unionModel)
      data.addNamedModel(tBoxName, withReasoning)
      if(!data.containsNamedModel(aBoxName))
      {
        data.addNamedModel(aBoxName, ModelFactory.createDefaultModel())
      }
      data.commit()
    }
      catch{case e: Exception ⇒ ErrorResult(e.getMessage)}
    finally data.end()

  }

  override def getABoxNoReasoning(): OntModel = {

    //val schema = data.getNamedModel(tBoxName)
    //val start = System.currentTimeMillis()

    val aBoxModel =  ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM,
      data.getNamedModel(aBoxName))
    //aBoxModel.addSubModel(schema)
    //DC.log("Execution of getABox took " + (System.currentTimeMillis() - start ))

    aBoxModel
    //val reasoner = ReasonerRegistry.getOWLMicroReasoner.bindSchema(schema)
    //ModelFactory.createInfModel(reasoner, aBoxModel)
  }

}

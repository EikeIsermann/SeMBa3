package app

import java.io.{BufferedWriter, File, FileOutputStream, OutputStreamWriter}

import globalConstants.SembaPaths
import org.apache.jena.ontology.{OntModel, OntModelSpec}
import org.apache.jena.query.{Dataset, ReadWrite}
import org.apache.jena.rdf.model.{Model, ModelFactory, Statement}
import org.apache.jena.tdb.{TDB, TDBFactory}
import org.apache.jena.vocabulary.OWL

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
object TDBTest extends App{
  var tdb: Dataset = TDBFactory.createDataset("/Users/uni/desktop/library/ontology")
  tdb.getContext.set(TDB.symUnionDefaultGraph, true)
  //addStatement("modelA", "itemA", "likes", "itemB")
  //addStatement("modelB", "itemC", "likes", "itemD")
  //addStatement("urn:x-arq:UnionGraph", "itemA", "likes", "itemC")
  export("tBox")
  export("aBox")
  tdb.begin(ReadWrite.READ)
  try{

   // println(ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF,tdb.getNamedModel("tBox")).getOntClass(SembaPaths.collectionClassURI).listSubClasses().toList)

  }
  finally {tdb.commit ; tdb.end() }





  def addStatement(modelName: String, sub: String, prop: String, obj: String): Unit ={
    tdb.begin(ReadWrite.WRITE)
    try{
      var model: OntModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF,tdb.getNamedModel(modelName))
      val subject = model.createIndividual(sub, OWL.Thing)
      val obje =  model.createIndividual(obj, OWL.Thing)
      val property = model.createObjectProperty(prop)
      subject.addProperty(property, obje)
    }
    finally{
      tdb.commit()
      tdb.end()
    }



  }

  def export(aModel: String) = {
    tdb.begin(ReadWrite.READ)
    try{
      var model: Model =tdb.getNamedModel(aModel)
      println("This model has " + model.size() + " Statements.")

      model.write(
        new BufferedWriter(
          new OutputStreamWriter(
            new FileOutputStream(
              new File("/Users/uni/desktop/exporttest/" + aModel +".xml" +
                ""), false
            )
          )
        ), "TURTLE"
      );
    }
    finally{tdb.commit(); tdb.end()}
  }



}

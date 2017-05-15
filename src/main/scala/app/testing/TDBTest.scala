package app.testing

import java.io.{BufferedWriter, File, FileOutputStream, OutputStreamWriter}

import org.apache.jena.ontology.{OntModel, OntModelSpec}
import org.apache.jena.query.{Dataset, ReadWrite}
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.jena.tdb.{TDB, TDBFactory}
import org.apache.jena.vocabulary.OWL

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
object TDBTest extends App{
  var tdb: Dataset = TDBFactory.createDataset("C:\\Users\\eikei_000\\Desktop\\sparqlLib\\ontology")
  //tdb.getContext.set(TDB.symUnionDefaultGraph, true)
  //addStatement("modelA", "itemA", "likes", "itemB")
  //addStatement("modelB", "itemC", "likes", "itemD")
  //addStatement("urn:x-arq:UnionGraph", "itemA", "likes", "itemC")
  //export("tBox")
  export("aBox")
  tdb.begin(ReadWrite.READ)
  try{

    //println(tdb.getNamedModel("aBox").add(tdb.getNamedModel("tBox")).size)
    //println(ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF,tdb.getNamedModel("aBox")).addSubModel(tdb.getName)

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
              new File("C:\\Users\\eikei_000\\Desktop\\tdbexport\\" + aModel +".xml" +
                ""), false
            )
          )
        ), "TURTLE"
      );
    }
    finally{tdb.commit(); tdb.end()}
  }



}

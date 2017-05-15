package app.testing

import sembaGRPC._

import scala.util.Random

/**
  * Created by Eike on 28.04.2017.
  */
object ClientTestApp extends App {
  val testApi = SembaConnectionImpl()
  val testLib = new ClientLib("file:///C:/Users/eikei_000/Desktop/library/library.ttl", testApi)

  for(i <- 1 to 300){
    testLib.addItem("file:///C:/Users/eikei_000/Desktop/testdata")
  }

  val iter = testLib.content.values.iterator
  for(i <- 1 to 10){
    testLib.getMetadata(iter.next.uri)
  }
  testLib.content.values.foreach(x => testLib.getMetadata(x.uri))
  Thread.sleep(100000000)


  val classes = testLib.concepts.collectionClasses.keySet.toArray
    classes.apply(Random.nextInt(classes.size - 1))
  val query =   "PREFIX semba: <http://www.hci.uni-wuerzburg.de/ontologies/semba/semba-main.owl#> " +
  "SELECT ?mediaItem"+
  "WHERE {" +
  "  ?x a  <file:///C:/Users/eikei_000/Desktop/library/library.ttl#generatedMetadata_meta:author>; "+
    " semba:hasValue \"Eike Isermann\";" +
  "   semba:describesItem  ?mediaItem . "+
  "}"

  testLib.sparqlToLibContent(ClientTestApp.query, Seq("?mediaItem"))
  val allCollections = List("da")

  val contentArray = testLib.concepts.collectionClasses.keySet.toArray
  contentArray.apply(Random.nextInt(contentArray.size - 1))

  //allCollections.foreach(
   //x => for(i <- 1 to 10) testLib.addToCollection(testLib.contentArray.apply(Random.nextInt(contentArray.size - 1)), x)
  //)

  testLib.openCollections.values.foreach(x => x.contents.values.foreach(collection1 => x.contents.values.foreach(collection2 => testLib.connectItems(collection1, collection2, "http://www.hci.uni-wuerzburg.de/ontologies/semba/semba-teaching.owl#preceeds" ))))
BenchData.metadataVariables.foreach(x => testLib.sparql(BenchData.metadataSparql(x._1,x._2), Seq("?y")))
  //val testLib = new ClientTestLib("file:///C:/Users/eikei_000/Desktop/library/library.ttl", ClientImpl("localhost", 50051))
  /*
  var counter = 0
  val queryString =
  {

    "SELECT ?x\nWHERE\n { ?x <file:///Users/uni/Desktop/library/library.ttl#Content-Type> \"image/jpeg\" ." +
      "   }"
  }

  val queryString2 =
  {

    "SELECT ?x\nWHERE\n { ?x <http://www.w3.org/2000/01/rdf-schema#subClassOf> <http://www.hci.uni-wuerzburg.de/ontologies/semba/semba-main.owl#Resource> ." +
      "   }"
  }

  val next = (value: UpdateMessage) => {
    println(value)
    println("Test")
    value.kindOfUpdate match {
      case UpdateType.NOTIFY =>
      case UpdateType.DELETE =>
      case UpdateType.REPLACE =>
      case UpdateType.ADD => println(value)
    }
  }






  val client = ClientImpl("localhost", 50051)
  var testSessionID: String = ""
  var test: LibraryConcepts = LibraryConcepts()
  testSessionID = client.registerSession().sessionID
  var testLib = Library(uri = "file:///C:/Users/eikei_000/Desktop/library/library.ttl")
  println(client.openLib(LibraryRequest().withLib(testLib).withSessionID(testSessionID)))
  //client.addItem("file:/users/uni/desktop/test.pdf", testLib)
  client.subscribeForUpdates(testSessionID, next)
  //println(client.getContent(testLib))
  //client.writeResults(testLib)
  //for (i <- 1 to 15) client.addItem("file:///C:/Users/eikei_000/Desktop/testdata/", testLib)
  for(i <- 1 to 0){
    client.addItem("file:/users/uni/desktop/test", testLib)
    val start = System.currentTimeMillis()
    val res  = client.sparql(queryString2, Seq("x"), testLib)
    println("Search took " + (System.currentTimeMillis() - start) + " Result size is: " + res.results.size )

    //client.getMetadata("file:///Users/uni/Desktop/library/library.ttl#f747fae9-a4d4-49e6-8861-7549d255d6c9", testLib)
  }
  tests()
  Thread.sleep(1200000)


  def tests()  ={


    //println(client.getContent(testLib))
    //    client.subscribeForUpdates(testSessionID)
    //println(client.addItem("file:/users/uni/documents/semba3/appdata/libraries/test/Test.jpeg", testLib))

    for(i <- 1 to 0){
      client.addItem("file:/users/uni/documents/semba3/appdata/libraries/test/", testLib)
    }
    println(client.addColl("Test", "http://www.hci.uni-wuerzburg.de/ontologies/semba/semba-teaching.owl#Course", testLib ))
    //println(client.getMetadata("file:///Users/uni/Documents/SeMBa3/appdata/libraries/data/Test/definition.ttl#content", testLib))
    //   println(client.getMetadata("file:///Users/uni/Documents/SeMBa3/appdata/libraries/data/Test_2/definition.ttl#content", testLib))
    //println(client.removeItem("file:///Users/uni/Documents/SeMBa3/appdata/libraries/data/Test_4/definition.ttl#content", testLib))
    //println(client.getContent(testLib))
    for(i <- 1 to 1){
      //println(client.sparql(queryString2,testLib))
      //println(client.sparql(queryString,testLib))

    }
    Thread.sleep(1200000)
  }
  */

}

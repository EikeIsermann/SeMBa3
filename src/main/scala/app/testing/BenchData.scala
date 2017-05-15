package app.testing

/**
  * Created by Eike on 08.05.2017.
  */
object BenchData {


  def metadataSparql(key: String, value: String) =  {"PREFIX semba: <http://www.hci.uni-wuerzburg.de/ontologies/semba/semba-main.owl#> " +
    "SELECT ?y \nWHERE\n {" +
    "  ?x a  <"+ key + ">; "+
    " semba:hasValue \""+ value +"\";" +
    " semba:describesItem ?y ." +
    "}"}
   val libUri = "file:///C:/Users/eikei_000/Desktop/sparqlLib/library.ttl#generatedMetadata_"
   def metadataVariables = Seq(
     (libUri + "meta:creation-date", "2016-11-02T09:24:46"),
     (libUri + "Content-Type", "application/pdf"),
     (libUri + "producer", "Microsoft® Office Word 2007"),
     (libUri + "Application-Name", "Microsoft Office Word"),
     (libUri + "meta:author" , "Nathan Prestopnik"),
     (libUri + "meta:author" , "Eike Isermann")
   )

  def collectionSparql(key: String) = {"PREFIX semba: <http://www.hci.uni-wuerzburg.de/ontologies/semba/semba-main.owl#>"+
    "PREFIX teaching: <http://www.hci.uni-wuerzburg.de/ontologies/semba/semba-teaching.owl#>"+
    "SELECT ?collection \nWHERE\n {"+
    "  ?collectionItem a semba:CollectionItem ;"+
    "  semba:linksToMediaItem <"+ key +"> ;"+
    "  teaching:preceeds ?anyItem ; "+
    "  semba:containedByCollection ?collection. "+
    "}"

  }


}

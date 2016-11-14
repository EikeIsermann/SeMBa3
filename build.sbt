name := "SeMBa3"

version := "1.0"

scalaVersion := "2.11.8"

javacOptions += "--verbose"

PB.targets in Compile := Seq(
  scalapb.gen(
    true,false,true,false) -> (sourceManaged in Compile).value
)


resolvers ++= Seq(
  "Typesafe Repo"             at "http://repo.typesafe.com/typesafe/repo/",
  "Typesafe Ivy Snapshots"    at "http://repo.typesafe.com/typesafe/ivy-snapshots/",
  "JBoss"         at "http://repository.jboss.org/nexus/content/groups/public-jboss/",
  "Sonatype Snapshots"        at "http://oss.sonatype.org/content/repositories/snapshots/",
  "Sonatype Releases"         at "http://oss.sonatype.org/content/repositories/releases/",
  "Sonatype Scala-Tools"      at "https://oss.sonatype.org/content/groups/scala-tools/",
  "Ice Maven Release Repository" at "http://anonsvn.icesoft.org/repo/maven2/releases"
)

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "org.scala-lang" % "scala-xml" % "2.11.0-M4",
  "org.apache.tika" % "tika-core" % "1.13",
  "org.apache.tika" % "tika-parsers" % "1.13",
  "org.apache.tika" % "tika-xmp" % "1.13",
  "uk.co.caprica" 			% "vlcj" 					% "3.10.1",
  "org.imgscalr"				% "imgscalr-lib"			% "4.2",
  "org.icepdf"				% "icepdf-core"				% "5.0.2_P01",
  "org.apache.jena"			% "apache-jena-libs" 		% "3.0.1",
  "com.trueaccord.scalapb" %% "scalapb-runtime" % com.trueaccord.scalapb.compiler.Version.scalapbVersion % "protobuf",
  "org.apache.commons" % "commons-lang3" % "3.3.2",
  "com.typesafe.akka" %% "akka-actor" % "2.4.7",
  "io.grpc" % "grpc-netty" % "1.0.1",
  "com.trueaccord.scalapb" %% "scalapb-runtime-grpc" % com.trueaccord.scalapb.compiler.Version.scalapbVersion,
  "com.typesafe.akka" %% "akka-agent" % "2.4.7"

)

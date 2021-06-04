name := "scala-ws"

version := "0.1"

//scalaVersion := "2.12.4"

//version of Scala
scalaVersion := "2.12.10"
// local version - scalaVersion := "2.11.8"
// spark library dependencies 
// change this to 3.0.0 when released
libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "3.0.0-preview2",
  "org.apache.spark" %% "spark-sql"  % "3.0.0-preview2"
)
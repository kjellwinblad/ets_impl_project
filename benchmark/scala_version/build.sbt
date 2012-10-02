name := "Moves"

version := "1.0"

scalaVersion := "2.10.0-M7"

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-actors" % "2.10.0-M7"
)
 
scalacOptions ++= Seq("-unchecked", "-deprecation")

mainClass := Some("moves.MultiMoves")


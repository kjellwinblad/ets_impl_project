name := "Moves"

version := "1.0"

scalaVersion := "2.9.2"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
 
scalacOptions ++= Seq("-unchecked", "-deprecation")

mainClass := Some("moves.MultiMoves")

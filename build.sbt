
name := "zookeeper-client"

organization := "io.metabookmarks"

bintrayOrganization := Some("metabookmarks")

licenses += ("Apache-2.0", url("http://opensource.org/licenses/Apache-2.0"))

scalaVersion := "2.12.2"

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint", "-Xlog-implicits")

scalacOptions := Seq("-deprecation", "-feature", "-language:postfixOps")

val zookeeperVersion = "3.5.3-beta"
val akkaVersion = "2.5.1"
val circeVersion = "0.8.0"

libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.25"

libraryDependencies += "org.slf4j" % "log4j-over-slf4j" % "1.7.25"

libraryDependencies ++= Seq("circe-core",
  "circe-generic",
  "circe-parser").map("io.circe" %% _ % circeVersion)

libraryDependencies += "org.apache.zookeeper" % "zookeeper" % zookeeperVersion exclude("org.apache", "log4j")  exclude("log4j", "log4j") exclude("org.slf4j", "slf4j-log4j12") artifacts(Artifact(""))

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.3" % "test"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.3" % "test"


fork in Test := true

cancelable in Global := true

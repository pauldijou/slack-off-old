name := "slack-off"

version := "1.0-SNAPSHOT"

resolvers += "Sonatype OSS Releases"  at "http://oss.sonatype.org/content/repositories/releases/"

resolvers += "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/maven-releases/"

libraryDependencies ++= Seq(
  cache,
  "joda-time" % "joda-time" % "2.3"
)

play.Project.playScalaSettings

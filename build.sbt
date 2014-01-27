name := "addon-provider-template"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "joda-time" % "joda-time" % "2.3",
  "org.postgresql" % "postgresql" % "9.3-1100-jdbc41",
  "commons-codec" % "commons-codec" % "1.9"
)

scalacOptions ++= Seq("-feature")

play.Project.playScalaSettings

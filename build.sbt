name := "addon-provider-template"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache
)

scalacOptions ++= Seq("-feature")

play.Project.playScalaSettings

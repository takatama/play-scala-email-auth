name := "play-scala-email-auth"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "com.github.t3hnar" % "scala-bcrypt_2.10" % "2.3",
  "com.typesafe" %% "play-plugins-mailer" % "2.1.0",
  filters
)     

play.Project.playScalaSettings

lazy val hre = (project in file(".")).settings(
  name := "hre",
  version := "0.1-SNAPSHOT",

  libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",
  libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3",

  // Disable documentation generation
  Compile / doc / sources := Nil,
  Compile / packageDoc / publishArtifact := false,
)

val spinalVersion = "1.14.0"

lazy val root = (project in file("."))
  .settings(
    name := "Simple-Oscillator",
    version := "0.1.0",
    scalaVersion := "2.12.18",
    libraryDependencies ++= Seq(
      "com.github.spinalhdl" %% "spinalhdl-core" % spinalVersion,
      "com.github.spinalhdl" %% "spinalhdl-lib" % spinalVersion,
      "org.scalatest" %% "scalatest" % "3.2.17" % "test",
      compilerPlugin("com.github.spinalhdl" %% "spinalhdl-idsl-plugin" % spinalVersion)
    )
  )

fork := true

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked"
)

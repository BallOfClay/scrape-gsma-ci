name := "scrapeGSMArena"

version := "0.1"

scalaVersion := "2.12.6"

libraryDependencies ++= Seq(
  "org.tpolecat" %% "doobie-core"      % "0.5.2",
  "org.tpolecat" %% "doobie-postgres"  % "0.5.2", // Postgres driver 42.2.2 + type mappings.
  "com.typesafe" % "config" % "1.3.2"
)

// addSbtPlugin("org.ensime" % "sbt-ensime" % "2.5.1")


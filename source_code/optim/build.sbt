name := "Spark GS"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
    "org.apache.spark" %% "spark-core" % "2.1.0",
    "org.apache.spark" %% "spark-sql" % "2.1.0",
    "org.scalanlp" % "breeze_2.11" % "0.12"
  )
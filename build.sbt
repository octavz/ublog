lazy val Versions = new {
  val kindProjector    = "0.11.0"
  val scalamacros      = "2.1.1"
  val zio              = "1.0.0-RC18-2"
  val zioInteropCats   = "2.0.0.0-RC12"
  val doobie           = "0.8.8"
  val pureconfig       = "0.12.3"
  val flyway           = "6.3.1"
  val testcontainers   = "0.36.1"
  val catsEffects      = "2.1.1"
  val betterMonadicFor = "0.3.1"
  val zioLogging       = "0.2.5"
  val akkaHttp         = "10.1.11"
  val akkaStream       = "2.5.26"
  val sprayJson        = "1.3.5"
}

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / scalaVersion      := "2.13.1"
ThisBuild / organization      := "org.github"
ThisBuild / scalafmtOnCompile := true
ThisBuild / turbo             := false
ThisBuild / resolvers ++= Seq(
  Opts.resolver.sonatypeSnapshots,
  Opts.resolver.sonatypeReleases
)
ThisBuild / scalacOptions := Seq(
  "-Ywarn-unused",
  "-Ywarn-numeric-widen",
  "-deprecation",
  "-Ywarn-value-discard",
  "-Ymacro-annotations"
  //"-Xfatal-warnings"
)

IntegrationTest / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat
lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    name := "ublog",
    libraryDependencies ++= deps,
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
    addCompilerPlugin(
      "com.olegpy" %% "better-monadic-for" % Versions.betterMonadicFor
    )
  )
  .settings(Defaults.itSettings)

// Scala libraries
val testDeps = Seq(
  "dev.zio"      %% "zio-test"                        % Versions.zio            % "test,it",
  "dev.zio"      %% "zio-test-magnolia"               % Versions.zio            % "test,it",
  "com.dimafeng" %% "testcontainers-scala"            % Versions.testcontainers % "test,it",
  "com.dimafeng" %% "testcontainers-scala-postgresql" % Versions.testcontainers % "test,it"
)

val deps = Seq(
  "com.typesafe.akka"     %% "akka-http"            % Versions.akkaHttp,
  "com.typesafe.akka"     %% "akka-http-spray-json" % Versions.akkaHttp,
  "com.typesafe.akka"     %% "akka-stream"          % Versions.akkaStream,
  "dev.zio"               %% "zio"                  % Versions.zio,
  "dev.zio"               %% "zio-test-sbt"         % Versions.zio,
  "dev.zio"               %% "zio-interop-cats"     % Versions.zioInteropCats,
  "org.tpolecat"          %% "doobie-core"          % Versions.doobie,
  "org.tpolecat"          %% "doobie-postgres"      % Versions.doobie,
  "org.tpolecat"          %% "doobie-hikari"        % Versions.doobie,
  "com.github.pureconfig"  %% "pureconfig"            % Versions.pureconfig,
  "org.flywaydb"           % "flyway-core"            % Versions.flyway,
  "dev.zio"               %% "zio-logging-slf4j"    % Versions.zioLogging,
  "io.spray"              %% "spray-json"           % Versions.sprayJson,
  "org.typelevel"         %% "cats-effect"          % Versions.catsEffects,
  "ch.qos.logback"              % "logback-classic"           % "1.2.3",
  "io.github.gaelrenoux"  %% "tranzactio"           % "0.3.0"
) ++ testDeps

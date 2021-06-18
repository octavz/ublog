lazy val Versions = new {
  val zio              = "1.0.9"
  val zioInteropCats   = "2.5.1.0"
  val doobie           = "0.13.4"
  val pureconfig       = "0.16.0"
  val flyway           = "7.10.0"
  val testcontainers   = "0.39.5"
  val catsEffects      = "2.5.1"
  val betterMonadicFor = "0.3.1"
  val zioLogging       = "0.5.10"
  val akkaHttp         = "10.2.4"
  val akkaStream       = "2.6.15"
  val sprayJson        = "1.3.6"
  val chimney          = "0.6.1"
  val kamon            = "2.2.0"
}

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / scalaVersion      := "2.13.6"
ThisBuild / organization      := "org.ublog"
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
  "dev.zio"        %% "zio-test"                        % Versions.zio % "test,it",
  "dev.zio"        %% "zio-test-magnolia"               % Versions.zio % "test,it",
  "com.dimafeng"   %% "testcontainers-scala"            % Versions.testcontainers % "test,it",
  "com.dimafeng"   %% "testcontainers-scala-postgresql" % Versions.testcontainers % "test,it",
  "ch.qos.logback" % "logback-classic"                  % "1.2.3"
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
  "com.github.pureconfig" %% "pureconfig"           % Versions.pureconfig,
  "org.flywaydb"          % "flyway-core"           % Versions.flyway,
  "dev.zio"               %% "zio-logging-slf4j"    % Versions.zioLogging,
  "io.spray"              %% "spray-json"           % Versions.sprayJson,
  "org.typelevel"         %% "cats-effect"          % Versions.catsEffects,
  "io.scalaland"          %% "chimney"              % Versions.chimney,
  "ch.qos.logback"        % "logback-classic"       % "1.2.3",
  "io.github.gaelrenoux"  %% "tranzactio"           % "2.0.0",
  "dev.profunktor"        %% "redis4cats-effects"   % "0.13.1",
  "io.kamon"              %% "kamon-bundle"         % Versions.kamon,
  "io.kamon"              %% "kamon-prometheus"     % Versions.kamon
) ++ testDeps

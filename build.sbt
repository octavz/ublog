lazy val Versions = new {
  val zio              = "1.0.10"
  val zioInteropCats   = "2.5.1.0"
  val zioLogging       = "0.5.11"
  val zioHttp          = "1.0.0.0-RC17"
  val zioJson          = "0.1.5"
  val doobie           = "0.13.4"
  val pureconfig       = "0.16.0"
  val flyway           = "7.11.4"
  val catsEffects      = "2.5.2"
  val tranzactio       = "2.1.0"
  val redis4Cats       = "0.14.0"
  val betterMonadicFor = "0.3.1"
  val akkaHttp         = "10.2.5"
  val akkaStream       = "2.6.15"
  val sprayJson        = "1.3.6"
  val chimney          = "0.6.1"
  val kamon            = "2.2.3"
  val logback          = "1.2.5"
  val testcontainers   = "0.39.5"
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
    libraryDependencies ++= compileDependencies,
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
    addCompilerPlugin(
      "com.olegpy" %% "better-monadic-for" % Versions.betterMonadicFor
    )
  )
  .settings(Defaults.itSettings)

// Scala libraries
val testDependencies = Seq(
  "dev.zio"      %% "zio-test"                        % Versions.zio            % "test,it",
  "dev.zio"      %% "zio-test-magnolia"               % Versions.zio            % "test,it",
  "com.dimafeng" %% "testcontainers-scala"            % Versions.testcontainers % "test,it",
  "com.dimafeng" %% "testcontainers-scala-postgresql" % Versions.testcontainers % "test,it"
)

val compileDependencies = Seq(
  "com.typesafe.akka"     %% "akka-http"            % Versions.akkaHttp,
  "com.typesafe.akka"     %% "akka-http-spray-json" % Versions.akkaHttp,
  "com.typesafe.akka"     %% "akka-stream"          % Versions.akkaStream,
  "dev.zio"               %% "zio"                  % Versions.zio,
  "dev.zio"               %% "zio-test-sbt"         % Versions.zio,
  "dev.zio"               %% "zio-interop-cats"     % Versions.zioInteropCats,
  "dev.zio"               %% "zio-logging-slf4j"    % Versions.zioLogging,
  "dev.zio"               %% "zio-json"             % Versions.zioJson,
  "io.d11"                %% "zhttp"                % Versions.zioHttp,
  "org.tpolecat"          %% "doobie-core"          % Versions.doobie,
  "org.tpolecat"          %% "doobie-postgres"      % Versions.doobie,
  "org.tpolecat"          %% "doobie-hikari"        % Versions.doobie,
  "com.github.pureconfig" %% "pureconfig"           % Versions.pureconfig,
  "io.spray"              %% "spray-json"           % Versions.sprayJson,
  "org.typelevel"         %% "cats-effect"          % Versions.catsEffects,
  "io.scalaland"          %% "chimney"              % Versions.chimney,
  "org.flywaydb"          % "flyway-core"           % Versions.flyway,
  "ch.qos.logback"        % "logback-classic"       % Versions.logback,
  "io.github.gaelrenoux"  %% "tranzactio"           % Versions.tranzactio,
  "dev.profunktor"        %% "redis4cats-effects"   % Versions.redis4Cats,
  "io.kamon"              %% "kamon-bundle"         % Versions.kamon,
  "io.kamon"              %% "kamon-prometheus"     % Versions.kamon
) ++ testDependencies

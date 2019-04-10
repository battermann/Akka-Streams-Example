name := "akka-streams-example"
scalaVersion := "2.12.8"

lazy val commonSettings = libraryDependencies ++= {
  val circeVersion = "0.11.1"
  Seq(
    "com.typesafe.akka"          %% "akka-stream"       % "2.5.22",
    "com.typesafe.akka"          %% "akka-stream-kafka" % "1.0-M1",
    "com.typesafe.scala-logging" %% "scala-logging"     % "3.9.0",
    "ch.qos.logback"             % "logback-classic"    % "1.2.3",
    "io.circe"                   %% "circe-core"        % circeVersion,
    "io.circe"                   %% "circe-generic"     % circeVersion,
    "io.circe"                   %% "circe-parser"      % circeVersion,
  )
}

lazy val fileLoader = (project in file("file-loader"))
  .enablePlugins(JavaServerAppPackaging)
  .enablePlugins(DockerPlugin)
  .settings(
    scalacOptions ++= flags,
    commonSettings
  )

lazy val summarizer = project
  .enablePlugins(JavaServerAppPackaging)
  .enablePlugins(DockerPlugin)
  .settings(
    scalacOptions ++= flags,
    commonSettings,
    libraryDependencies ++= {
      Seq(
        "org.flywaydb"               % "flyway-core"             % "5.2.4",
        "com.github.cb372"           %% "cats-retry-core"        % "0.2.5",
        "com.github.cb372"           %% "cats-retry-cats-effect" % "0.2.5",
        "org.postgresql"             % "postgresql"              % "42.2.5.jre7",
        "org.tpolecat"               %% "doobie-core"            % "0.6.0",
        "org.tpolecat"               %% "doobie-postgres"        % "0.6.0",
        "com.github.julien-truffaut" %% "newts-core"             % "0.3.2"
      )
    }
  )

lazy val api = project
  .enablePlugins(JavaServerAppPackaging)
  .enablePlugins(DockerPlugin)
  .settings(
    scalacOptions ++= flags,
    commonSettings,
    libraryDependencies ++= {
      val http4sVersion = "0.19.0"
      Seq(
        "org.postgresql" % "postgresql"           % "42.2.5.jre7",
        "org.tpolecat"   %% "doobie-core"         % "0.6.0",
        "org.tpolecat"   %% "doobie-postgres"     % "0.6.0",
        "org.http4s"     %% "http4s-dsl"          % http4sVersion,
        "org.http4s"     %% "http4s-blaze-server" % http4sVersion,
        "org.http4s"     %% "http4s-blaze-client" % http4sVersion,
        "org.http4s"     %% "http4s-circe"        % http4sVersion,
      )
    }
  )

lazy val flags =  Seq(
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-encoding",
  "utf-8", // Specify character encoding used by source files.
  "-explaintypes", // Explain type errors in more detail.
  "-feature", // Emit warning and location for usages of features that should be imported explicitly.
  "-language:existentials", // Existential types (besides wildcard types) can be written and inferred
  "-language:experimental.macros", // Allow macro definition (besides implementation and application)
  "-language:higherKinds", // Allow higher-kinded types
  "-language:implicitConversions", // Allow definition of implicit functions called views
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.
  "-Xcheckinit", // Wrap field accessors to throw an exception on uninitialized access.
  "-Xfatal-warnings", // Fail the compilation if there are any warnings.
  "-Xfuture", // Turn on future language features.
  "-Xlint:adapted-args", // Warn if an argument list is modified to match the receiver.
  "-Xlint:by-name-right-associative", // By-name parameter of right associative operator.
  "-Xlint:constant", // Evaluation of a constant arithmetic expression results in an error.
  "-Xlint:delayedinit-select", // Selecting member of DelayedInit.
  "-Xlint:doc-detached", // A Scaladoc comment appears to be detached from its element.
  "-Xlint:inaccessible", // Warn about inaccessible types in method signatures.
  "-Xlint:infer-any", // Warn when a type argument is inferred to be `Any`.
  "-Xlint:missing-interpolator", // A string literal appears to be missing an interpolator id.
  "-Xlint:nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Xlint:nullary-unit", // Warn when nullary methods return Unit.
  "-Xlint:option-implicit", // Option.apply used implicit view.
  "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
  "-Xlint:private-shadow", // A private field (or class parameter) shadows a superclass field.
  "-Xlint:stars-align", // Pattern sequence wildcard must align with sequence component.
  "-Xlint:type-parameter-shadow", // A local type parameter shadows a type already in scope.
  "-Xlint:unsound-match", // Pattern match may not be typesafe.
  "-Yno-adapted-args", // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
  "-Ypartial-unification", // Enable partial unification in type constructor inference
  "-Ywarn-dead-code", // Warn when dead code is identified.
  "-Ywarn-extra-implicit", // Warn when more than one implicit parameter section is defined.
  "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
  "-Ywarn-infer-any", // Warn when a type argument is inferred to be `Any`.
  "-Ywarn-nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Ywarn-nullary-unit", // Warn when nullary methods return Unit.
  "-Ywarn-numeric-widen", // Warn when numerics are widened.
  "-Ywarn-unused:implicits", // Warn if an implicit parameter is unused.
  "-Ywarn-unused:imports", // Warn if an import selector is not referenced.
  "-Ywarn-unused:locals", // Warn if a local definition is unused.
  "-Ywarn-unused:params", // Warn if a value parameter is unused.
  "-Ywarn-unused:patvars", // Warn if a variable bound in a pattern is unused.
  "-Ywarn-unused:privates", // Warn if a private member is unused.
  "-Ywarn-value-discard" // Warn when non-Unit expression results are unused.
)

import play.sbt.routes.RoutesKeys
import uk.gov.hmrc.DefaultBuildSettings
import sbt.Keys.*
import sbt.*
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "business-matching"

ThisBuild / majorVersion := 2
ThisBuild / scalaVersion := "3.3.7"

lazy val playSettings : Seq[Setting[?]] = Seq.empty

lazy val plugins : Seq[Plugins] = Seq(play.sbt.PlayScala, SbtDistributablesPlugin)

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    ScoverageKeys.coverageExcludedPackages :=  "<empty>;Reverse.*;views.html.*;app.Routes.*;prod.*;uk.gov.hmrc.*;testOnlyDoNotUseInAppConf.*;forms.*;config.*;",
    ScoverageKeys.coverageMinimumStmtTotal := 95,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(plugins *)
  .settings((playSettings ++ scoverageSettings) *)
  .settings(
    RoutesKeys.routesImport := Seq.empty,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    Test / parallelExecution := false,
    Test / fork := true,
    retrieveManaged := true,
    routesGenerator := InjectedRoutesGenerator,
    scalacOptions += "-Wconf:src=.*routes.*:s",
    scalacOptions ~= (_.distinct)
  )
  .settings(
    resolvers ++= Seq(
      Resolver.typesafeRepo("releases")
    )
  )

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.itDependencies)

addCommandAlias("runAllChecks", ";clean;compile;coverage;test;it/test;coverageReport")


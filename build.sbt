import play.sbt.routes.RoutesKeys
import sbt.Keys._
import sbt._
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "business-matching"

lazy val appDependencies: Seq[ModuleID] = AppDependencies()
lazy val playSettings : Seq[Setting[_]] = Seq.empty

lazy val plugins : Seq[Plugins] = Seq(play.sbt.PlayScala, SbtAutoBuildPlugin,
  SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    ScoverageKeys.coverageExcludedPackages :=  "<empty>;Reverse.*;views.html.*;app.Routes.*;prod.*;uk.gov.hmrc.*;testOnlyDoNotUseInAppConf.*;forms.*;config.*;",
    ScoverageKeys.coverageMinimum := 100,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(plugins : _*)
  .settings(playSettings ++ scoverageSettings : _*)
  .settings(publishingSettings: _*)
  .configs(IntegrationTest)
  .settings(
    addTestReportOption(IntegrationTest, "int-test-reports"),
    inConfig(IntegrationTest)(Defaults.itSettings),
    RoutesKeys.routesImport := Seq.empty,
    scalaVersion := "2.12.11",
    targetJvm := "jvm-1.8",
    majorVersion := 2,
    libraryDependencies ++= appDependencies,
    parallelExecution in Test := false,
    fork in Test := true,
    retrieveManaged := true,
    routesGenerator := InjectedRoutesGenerator,
    Keys.fork                  in IntegrationTest :=  false,
    unmanagedSourceDirectories in IntegrationTest :=  (baseDirectory in IntegrationTest)(base => Seq(base / "it")).value,
    parallelExecution in IntegrationTest := false
  )
  .settings(
    resolvers := Seq(
      Resolver.bintrayRepo("hmrc", "releases"),
      Resolver.typesafeRepo("releases"),
      Resolver.jcenterRepo
    )
  )

import play.sbt.routes.RoutesKeys
import sbt.Keys._
import sbt._
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "business-matching"

lazy val appDependencies: Seq[ModuleID] = AppDependencies()
lazy val playSettings : Seq[Setting[_]] = Seq.empty

lazy val plugins : Seq[Plugins] = Seq(play.sbt.PlayScala, SbtDistributablesPlugin)

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    ScoverageKeys.coverageExcludedPackages :=  "<empty>;Reverse.*;views.html.*;app.Routes.*;prod.*;uk.gov.hmrc.*;testOnlyDoNotUseInAppConf.*;forms.*;config.*;",
    ScoverageKeys.coverageMinimumStmtTotal := 100,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(plugins : _*)
  .settings(playSettings ++ scoverageSettings : _*)
  .configs(IntegrationTest)
  .settings(
    addTestReportOption(IntegrationTest, "int-test-reports"),
    inConfig(IntegrationTest)(Defaults.itSettings),
    RoutesKeys.routesImport := Seq.empty,
    scalaVersion := "2.13.8",
    majorVersion := 2,
    libraryDependencies ++= appDependencies,
    Test / parallelExecution := false,
    Test / fork := true,
    retrieveManaged := true,
    routesGenerator := InjectedRoutesGenerator,
    IntegrationTest / Keys.fork :=  false,
    IntegrationTest / unmanagedSourceDirectories := (IntegrationTest / baseDirectory)(base => Seq(base / "it")).value,
    IntegrationTest / parallelExecution := false,
    scalacOptions += "-Wconf:src=routes/.*:s"
  )
  .settings(
    resolvers ++= Seq(
      Resolver.typesafeRepo("releases"),
      Resolver.jcenterRepo
    )
  )

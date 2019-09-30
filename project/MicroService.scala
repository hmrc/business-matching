import play.sbt.routes.RoutesKeys._
import sbt.Keys._
import sbt._
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, targetJvm}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion
import uk.gov.hmrc.{SbtArtifactory, SbtAutoBuildPlugin}

trait MicroService {
  import TestPhases.oneForkedJvmPerTest

  val appName: String = "business-matching"
  val appDependencies : Seq[ModuleID]

  lazy val plugins : Seq[Plugins] = Seq(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
  lazy val playSettings : Seq[Setting[_]] = Seq.empty

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
      scalaVersion := "2.11.12",
      targetJvm := "jvm-1.8",
      majorVersion := 2,
      libraryDependencies ++= appDependencies,
      parallelExecution in Test := false,
      fork in Test := true,
      retrieveManaged := true,
      routesGenerator := InjectedRoutesGenerator,
      Keys.fork                  in IntegrationTest :=  false,
      unmanagedSourceDirectories in IntegrationTest :=  (baseDirectory in IntegrationTest)(base => Seq(base / "it")).value,
      testGrouping               in IntegrationTest :=  oneForkedJvmPerTest((definedTests in IntegrationTest).value),
      parallelExecution in IntegrationTest := false
    )
    .settings(
      resolvers := Seq(
        Resolver.bintrayRepo("hmrc", "releases"),
        Resolver.typesafeRepo("releases"),
        Resolver.jcenterRepo
      )
    )
}

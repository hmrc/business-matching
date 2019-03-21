import play.sbt.routes.RoutesKeys._
import sbt.Keys._
import sbt._
import uk.gov.hmrc.DefaultBuildSettings.targetJvm
import uk.gov.hmrc.{SbtArtifactory, SbtAutoBuildPlugin, _}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion
import play.routes.compiler.StaticRoutesGenerator


trait MicroService {


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
    .settings(
      scalaVersion := "2.11.11",
      targetJvm := "jvm-1.8",
      majorVersion := 2,
      libraryDependencies ++= appDependencies,
      parallelExecution in Test := false,
      fork in Test := true,
      retrieveManaged := true,
      routesGenerator := StaticRoutesGenerator
    )
    .settings(
      resolvers := Seq(
        Resolver.bintrayRepo("hmrc", "releases"),
        Resolver.typesafeRepo("releases"),
        Resolver.jcenterRepo
      )
    )
}

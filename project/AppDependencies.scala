import play.core.PlayVersion
import sbt._
import play.sbt.PlayImport.ws

private object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-26" % "1.7.0",
    "uk.gov.hmrc" %% "domain"            % "5.6.0-play-26"
  )

  trait TestDependencies {
    lazy val scope: String = "it,test"
    val test : Seq[ModuleID]
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test: Seq[ModuleID] = Seq(
        "org.scalatest"           %% "scalatest"          % "3.0.8"             % scope,
        "org.scalatestplus.play"  %% "scalatestplus-play" % "3.1.3"             % scope,
        "org.pegdown"              % "pegdown"            % "1.6.0"             % scope,
        "com.typesafe.play"       %% "play-test"          % PlayVersion.current % scope,
        "org.mockito"              % "mockito-all"        % "1.10.19"           % scope,
        "com.github.tomakehurst"   % "wiremock-jre8"      % "2.26.3"            % IntegrationTest withSources()
      )
    }.test
  }

  def apply() = compile ++ Test()
}

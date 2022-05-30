import play.core.PlayVersion
import sbt._
import play.sbt.PlayImport.ws

private object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-backend-play-28" % "5.24.0",
    "uk.gov.hmrc" %% "domain"                    % "8.1.0-play-28"
  )

  trait TestDependencies {
    lazy val scope: String = "it,test"
    val test : Seq[ModuleID]
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test: Seq[ModuleID] = Seq(
        "org.scalatestplus.play"       %% "scalatestplus-play"     % "5.1.0"             % scope,
        "com.typesafe.play"            %% "play-test"              % PlayVersion.current % scope,
        "org.mockito"                  %  "mockito-core"           % "4.6.0"             % scope,
        "com.github.tomakehurst"       %  "wiremock-jre8"          % "2.33.2"            % scope,
        "com.fasterxml.jackson.module" %% "jackson-module-scala"   % "2.13.3"            % scope,
        "uk.gov.hmrc"                  %% "bootstrap-test-play-28" % "5.24.0"            % scope
      )
    }.test
  }

  def apply() = compile ++ Test()
}

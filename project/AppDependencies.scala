import play.core.PlayVersion
import sbt._
import play.sbt.PlayImport.ws

private object AppDependencies {

  val bootstrapVersion = "8.4.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-backend-play-30" % bootstrapVersion,
    "uk.gov.hmrc" %% "domain-play-30"            % "9.0.0"
  )

  trait TestDependencies {
    lazy val scope: String = "it,test"
    val test : Seq[ModuleID]
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test: Seq[ModuleID] = Seq(
        "org.mockito"                  %  "mockito-core"           % "5.4.0"             % scope,
        "com.github.tomakehurst"       %  "wiremock-jre8"          % "2.35.0"            % scope,
        "com.fasterxml.jackson.module" %% "jackson-module-scala"   % "2.15.2"            % scope,
        "uk.gov.hmrc"                  %% "bootstrap-test-play-30" % bootstrapVersion    % scope
      )
    }.test
  }

  def apply() = compile ++ Test()
}

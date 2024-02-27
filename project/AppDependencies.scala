import play.sbt.PlayImport.ws
import sbt.*

private object AppDependencies {

  val bootstrapVersion = "8.4.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-backend-play-30" % bootstrapVersion,
    "uk.gov.hmrc" %% "domain-play-30"            % "9.0.0"
  )

  val test: Seq[ModuleID] = Seq(
    "org.mockito"  %  "mockito-core"           % "5.10.0"         % Test,
    "uk.gov.hmrc"  %% "bootstrap-test-play-30" % bootstrapVersion % Test
  )

  val itDependencies: Seq[ModuleID] = Seq(
    "org.wiremock"                  %  "wiremock"             % "3.4.2"  % Test,
    "com.fasterxml.jackson.module"  %% "jackson-module-scala" % "2.16.1" % Test,
  )
}

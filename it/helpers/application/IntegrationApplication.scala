
package helpers.application

import connectors.{DefaultEtmpConnector, EtmpConnector}
import helpers.wiremock.WireMockConfig
import metrics.{DefaultServiceMetrics, ServiceMetrics}
import org.scalatest.TestSuite
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.test.Injecting
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import play.api.inject.{bind => playBind}

trait IntegrationApplication extends GuiceOneServerPerSuite with WireMockConfig with Injecting {
  self: TestSuite =>

  val currentAppBaseUrl: String = "business-matching"
  val testAppUrl: String        = s"http://localhost:$port"

  lazy val ws: WSClient = inject[WSClient]

  override lazy val app: Application = new GuiceApplicationBuilder()
    .overrides(playBind[EtmpConnector].to[DefaultEtmpConnector])
    .overrides(playBind[PlayAuthConnector].to[DefaultAuthConnector])
    .overrides(playBind[HttpClient].to[DefaultHttpClient])
    .overrides(playBind[ServiceMetrics].to[DefaultServiceMetrics])
    .configure(
      Map(
        "application.router"                                  -> "testOnlyDoNotUseInAppConf.Routes",
        "microservice.metrics.graphite.host"                  -> "localhost",
        "microservice.metrics.graphite.port"                  -> 2003,
        "microservice.metrics.graphite.prefix"                -> "play.business-matching.",
        "microservice.metrics.graphite.enabled"               -> true,
        "microservice.services.etmp-hod.host"                 -> wireMockHost,
        "microservice.services.etmp-hod.port"                 -> wireMockPort,
        "metrics.name"                                        -> "business-matching",
        "metrics.rateUnit"                                    -> "SECONDS",
        "metrics.durationUnit"                                -> "SECONDS",
        "metrics.showSamples"                                 -> true,
        "metrics.jvm"                                         -> true,
        "metrics.enabled"                                     -> true
      )
    ).build()

  def makeRequest(uri: String): WSRequest = ws.url(s"http://localhost:$port/$uri")
}

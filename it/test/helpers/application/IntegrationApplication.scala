/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        "play.http.router"                                    -> "testOnlyDoNotUseInAppConf.Routes",
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

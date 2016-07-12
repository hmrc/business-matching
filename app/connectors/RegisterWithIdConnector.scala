/*
 * Copyright 2016 HM Revenue & Customs
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

package connectors

import config.WSHttp
import metrics.Metrics
import metrics.MetricsEnum.REGISTER_WITH_ID_MATCH
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.JsValue
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.Authorization
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpPost, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


trait RegisterWithIdConnector extends ServicesConfig with RawResponseReads {

  def http: HttpGet with HttpPost

  def metrics: Metrics

  def serviceURL: String

  def baseURI: String

  def bearerToken: String

  def version: String

  def lookup(lookupData: JsValue): Future[HttpResponse] = {
    val postUrl = s"""$serviceURL/$baseURI/registerwithid/${version}"""
    implicit val hc = new HeaderCarrier(authorization = Some(Authorization("Bearer " + bearerToken)))
    Logger.debug("[RegisterWithIdConnector][lookup] lookup data:" + lookupData.toString)
    val timerContext = metrics.startTimer(REGISTER_WITH_ID_MATCH)

    http.POST[JsValue, HttpResponse](postUrl, lookupData) map {
      response =>
        timerContext.stop()
        Logger.debug(s"[RegisterWithIdConnector][lookup] - status: ${response.status}, responseBody: ${response.body}")
        response.status match {
          case Status.OK =>
            metrics.incrementSuccessCounter(REGISTER_WITH_ID_MATCH)
            response
          case _ =>
            Logger.warn(s"[RegisterWithIdConnector][lookup] - status: ${response.status}, responseBody: ${response.body}")
            metrics.incrementFailedCounter(REGISTER_WITH_ID_MATCH)
            response
        }
    }
  }
}

object RegisterWithIdConnector extends RegisterWithIdConnector {
  val http = WSHttp
  val metrics: Metrics = Metrics
  val serviceURL = baseUrl("register-with-id")
  val version = getConfString("register-with-id.version", "")
  val baseURI = "registrations"
  val bearerToken = getConfString("register-with-id.authorization-token", "")
}

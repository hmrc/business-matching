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

import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.JsValue
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
import config.WSHttp
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.logging.Authorization
import scala.concurrent.ExecutionContext.Implicits.global
import metrics.{MetricsEnum, Metrics}

import scala.concurrent.Future

trait EtmpConnector extends ServicesConfig with RawResponseReads {

  lazy val serviceURL = baseUrl("etmp-hod")
  val baseURI = "registration"
  val indLookupURI = "individual"
  val orgLookupURI = "organisation"
  val urlHeaderEnvironment: String
  val urlHeaderAuthorization: String

  val http: HttpGet with HttpPost = WSHttp
  def metrics: Metrics

  def lookup(lookupData: JsValue, userType: String, utr: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    implicit val headerCarrier = createHeaderCarrier
    Logger.debug(s"[EtmpConnector][lookup] utr: $utr userType: $userType, lookup: $lookupData")
    val timerContext = metrics.startTimer(MetricsEnum.ETMP_BUSINESS_MATCH)
    val result = userType match {
      case "sa" =>  http.POST[JsValue, HttpResponse]( s"""$serviceURL/$baseURI/$indLookupURI/$utr""", lookupData)
      case "org" => http.POST[JsValue, HttpResponse]( s"""$serviceURL/$baseURI/$orgLookupURI/$utr""", lookupData)
      case _ => throw new RuntimeException("Wrong user type!!")
    }
    result.map { response =>
      timerContext.stop()
      response.status match {
        case OK =>
          metrics.incrementSuccessCounter(MetricsEnum.ETMP_BUSINESS_MATCH)
          response
        case status =>
          metrics.incrementFailedCounter(MetricsEnum.ETMP_BUSINESS_MATCH)
          Logger.warn(s"[EtmpConnector][lookup] - status: $status InternalServerException ${response.body}")
          response
      }
    }
  }

  def createHeaderCarrier: HeaderCarrier = {
    HeaderCarrier(extraHeaders = Seq("Environment" -> urlHeaderEnvironment), authorization = Some(Authorization(urlHeaderAuthorization)))
  }

}

object EtmpConnector extends EtmpConnector {
  override val urlHeaderEnvironment: String = config("etmp-hod").getString("environment").getOrElse("")
  override val urlHeaderAuthorization: String = s"Bearer ${config("etmp-hod").getString("authorization-token").getOrElse("")}"
  override def metrics = Metrics
}

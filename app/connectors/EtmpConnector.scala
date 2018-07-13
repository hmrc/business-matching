/*
 * Copyright 2018 HM Revenue & Customs
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
import uk.gov.hmrc.play.config.ServicesConfig
import config.{BusinessMatchingGlobal, WSHttp}

import scala.concurrent.ExecutionContext.Implicits.global
import metrics.{Metrics, MetricsEnum}
import uk.gov.hmrc.play.audit.model.{Audit, DataEvent}
import uk.gov.hmrc.play.audit.AuditExtensions._

import scala.concurrent.Future
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.Authorization

trait EtmpConnector extends ServicesConfig with RawResponseReads {

  def serviceUrl: String

  def indLookupURI: String

  def orgLookupURI: String

  def urlHeaderEnvironment: String

  def urlHeaderAuthorization: String

  def http: CoreGet with CorePost

  def metrics: Metrics

  def audit = new Audit("business-matching", BusinessMatchingGlobal.auditConnector)

  def lookup(lookupData: JsValue, userType: String, utr: String): Future[HttpResponse] = {
    implicit val hc: HeaderCarrier = createHeaderCarrier
    val timerContext = metrics.startTimer(MetricsEnum.ETMP_BUSINESS_MATCH)
    val result = userType match {
      case "sa" => http.POST[JsValue, HttpResponse](s"$serviceUrl/$indLookupURI/$utr", lookupData)
      case "org" => http.POST[JsValue, HttpResponse](s"$serviceUrl/$orgLookupURI/$utr", lookupData)
      case _ => throw new RuntimeException("Wrong user type!!")
    }
    result.map { response =>
      timerContext.stop()
      response.status match {
        case OK =>
          metrics.incrementSuccessCounter(MetricsEnum.ETMP_BUSINESS_MATCH)
          response
        case NOT_FOUND => response
        case status =>
          metrics.incrementFailedCounter(MetricsEnum.ETMP_BUSINESS_MATCH)
          Logger.warn(s"[EtmpConnector][lookup] - status: $status")
          doFailedAudit("lookupFailed", lookupData.toString, response.body)
          response
      }
    }
  }

  def createHeaderCarrier: HeaderCarrier =
    HeaderCarrier(extraHeaders = Seq("Environment" -> urlHeaderEnvironment), authorization = Some(Authorization(urlHeaderAuthorization)))

  def doFailedAudit(auditType: String, request: String, response: String)(implicit hc:HeaderCarrier): Unit = {
    val auditDetails = Map("request" -> request,
                           "response" -> response)

    audit.sendDataEvent(DataEvent("business-matching", auditType,
      tags = hc.toAuditTags("", "N/A"),
      detail = hc.toAuditDetails(auditDetails.toSeq: _*)))
  }
}

object EtmpConnector extends EtmpConnector {
  val serviceUrl = baseUrl("etmp-hod")
  val indLookupURI: String = "registration/individual"
  val orgLookupURI: String = "registration/organisation"
  val urlHeaderEnvironment: String = config("etmp-hod").getString("environment").getOrElse("")
  val urlHeaderAuthorization: String = s"Bearer ${config("etmp-hod").getString("authorization-token").getOrElse("")}"
  val http: CoreGet with CorePost = WSHttp
  val metrics = Metrics
}

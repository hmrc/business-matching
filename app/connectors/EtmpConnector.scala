/*
 * Copyright 2023 HM Revenue & Customs
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

import metrics.{MetricsEnum, ServiceMetrics}
import play.api.http.Status._
import play.api.libs.json.JsValue
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{Audit, DataEvent}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.LoggerUtil._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DefaultEtmpConnector @Inject()(val servicesConfig: ServicesConfig,
                                     val http: HttpClientV2,
                                     val auditConnector: AuditConnector,
                                     val metrics: ServiceMetrics)(implicit val ec: ExecutionContext) extends EtmpConnector {
  val serviceUrl: String = servicesConfig.baseUrl("etmp-hod")
  val indLookupURI: String = "registration/individual"
  val orgLookupURI: String = "registration/organisation"
  val urlHeaderEnvironment: String = servicesConfig.getConfString("etmp-hod.environment", "")
  val urlHeaderAuthorization: String = s"Bearer ${servicesConfig.getConfString("etmp-hod.authorization-token", "")}"
}

trait EtmpConnector extends RawResponseReads {
  implicit val ec: ExecutionContext
  def serviceUrl: String
  def indLookupURI: String
  def orgLookupURI: String
  def urlHeaderEnvironment: String
  def urlHeaderAuthorization: String
  def auditConnector: AuditConnector

  def http: HttpClientV2
  def metrics: ServiceMetrics
  def audit = new Audit("business-matching", auditConnector)

  def lookup(lookupData: JsValue, userType: String, utr: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val timerContext = metrics.startTimer(MetricsEnum.ETMP_BUSINESS_MATCH)

    val uri = userType match {
      case "sa"     => s"$indLookupURI"
      case "org"    => s"$orgLookupURI"
      case _        => throw new RuntimeException("[EtmpConnector][lookup] Incorrect user type")
    }

    val url = s"$serviceUrl/$uri/$utr"

    http.post(url"$url").withBody(lookupData).setHeader(createHeaders : _*).execute[HttpResponse].map { response =>
      timerContext.stop()
      response.status match {
        case OK =>
          metrics.incrementSuccessCounter(MetricsEnum.ETMP_BUSINESS_MATCH)
          response
        case NOT_FOUND => response
        case status =>
          metrics.incrementFailedCounter(MetricsEnum.ETMP_BUSINESS_MATCH)
          logWarn(s"[EtmpConnector][lookup] - status: $status")
          doFailedAudit("lookupFailed", lookupData.toString, response.body)
          response
      }
    }
  }

  def createHeaders: Seq[(String, String)] = {
    Seq(
      "Environment"   -> urlHeaderEnvironment,
      "Authorization" -> urlHeaderAuthorization
    )
  }

  def doFailedAudit(auditType: String, request: String, response: String)(implicit hc: HeaderCarrier): Unit = {
    val auditDetails = Map("request" -> request,
                           "response" -> response)

    audit.sendDataEvent(DataEvent("business-matching", auditType,
      tags = hc.toAuditTags("", "N/A"),
      detail = hc.toAuditDetails(auditDetails.toSeq: _*)))
  }
}

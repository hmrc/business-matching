/*
 * Copyright 2020 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import metrics.{MetricsEnum, ServiceMetrics}
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.JsValue
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.{Audit, DataEvent}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class DefaultEtmpConnector @Inject()(val servicesConfig: ServicesConfig,
                                     val http: HttpClient,
                                     val auditConnector: AuditConnector,
                                     val metrics: ServiceMetrics) extends EtmpConnector {
  val serviceUrl: String = servicesConfig.baseUrl("etmp-hod")
  val indLookupURI: String = "registration/individual"
  val orgLookupURI: String = "registration/organisation"
  val urlHeaderEnvironment: String = servicesConfig.getConfString("etmp-hod.environment", "")
  val urlHeaderAuthorization: String = s"Bearer ${servicesConfig.getConfString("etmp-hod.authorization-token", "")}"
}

trait EtmpConnector extends RawResponseReads with Logging {
  def serviceUrl: String
  def indLookupURI: String
  def orgLookupURI: String
  def urlHeaderEnvironment: String
  def urlHeaderAuthorization: String
  def auditConnector: AuditConnector

  def http: CoreGet with CorePost
  def metrics: ServiceMetrics
  def audit = new Audit("business-matching", auditConnector)

  def lookup(lookupData: JsValue, userType: String, utr: String): Future[HttpResponse] = {
    implicit val hc: HeaderCarrier = createHeaderCarrier
    val timerContext = metrics.startTimer(MetricsEnum.ETMP_BUSINESS_MATCH)

    val uri = userType match {
      case "sa"     => s"$indLookupURI"
      case "org"    => s"$orgLookupURI"
      case _        => throw new RuntimeException("[EtmpConnector][lookup] Incorrect user type")
    }

    http.POST[JsValue, HttpResponse](s"$serviceUrl/$uri/$utr", lookupData).map { response =>
      timerContext.stop()
      response.status match {
        case OK =>
          metrics.incrementSuccessCounter(MetricsEnum.ETMP_BUSINESS_MATCH)
          response
        case NOT_FOUND => response
        case status =>
          metrics.incrementFailedCounter(MetricsEnum.ETMP_BUSINESS_MATCH)
          logger.warn(s"[EtmpConnector][lookup] - status: $status")
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

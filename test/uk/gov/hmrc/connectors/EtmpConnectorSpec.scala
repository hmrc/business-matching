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

package uk.gov.hmrc.connectors

import java.util.UUID

import connectors.EtmpConnector
import metrics.Metrics
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.domain.SaUtrGenerator
import uk.gov.hmrc.play.audit.http.config.LoadAuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{AppName, RunMode}
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.http.ws.{WSGet, WSPost}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpPost, HttpResponse}

import scala.concurrent.Future

class EtmpConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfter {

  object TestAuditConnector extends AuditConnector with AppName with RunMode {
    override lazy val auditingConfig = LoadAuditingConfig(s"$env.auditing")
  }

  class MockHttp extends WSGet with WSPost {
    override val hooks = NoneRequired
  }

  val mockWSHttp = mock[MockHttp]

  object TestEtmpConnector extends EtmpConnector {
    override val serviceUrl = ""
    override val indLookupURI: String = "registration/individual"
    override val orgLookupURI: String = "registration/organisation"
    override val http: HttpGet with HttpPost = mockWSHttp
    override val urlHeaderEnvironment: String = config("etmp-hod").getString("environment").getOrElse("")
    override val urlHeaderAuthorization: String = s"Bearer ${config("etmp-hod").getString("authorization-token").getOrElse("")}"
    override val metrics = Metrics
  }

  before {
    reset(mockWSHttp)
  }

  val saUserType = "sa"
  val orgUserType = "org"
  val matchUtr = new SaUtrGenerator().nextSaUtr
  val noMatchUtr = new SaUtrGenerator().nextSaUtr

  "BusinessCustomerConnector" must {

    "use correct metrics" in {
      EtmpConnector.metrics must be(Metrics)
    }

    "userType=sa" must {

      val matchSuccessResponse = Json.parse(
        """
          |{
          |  "businessName":"ACME",
          |  "businessType":"Unincorporated body",
          |  "businessAddress":"23 High Street\nPark View\nThe Park\nGloucester\nGloucestershire\nABC 123",
          |  "businessTelephone":"201234567890",
          |  "businessEmail":"contact@acme.com"
          |}
        """.stripMargin)

      val matchFailureResponse = Json.parse( """{"error": "Sorry. Business details not found."}""")

      "for a successful match, return business details" in {

        val inputJsonForUIB = Json.parse(
          s"""
             |{
             |  "businessType": "UIB",
             |  "uibCompany": {
             |    "uibBusinessName": "ACME",
             |    "uibCotaxAUTR": $matchUtr
             |  }
             |}
          """.stripMargin)

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

        when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())) thenReturn {
          Future.successful(HttpResponse(200, responseJson = Some(matchSuccessResponse)))
        }
        val result = TestEtmpConnector.lookup(inputJsonForUIB, saUserType, matchUtr.toString)
        await(result).json must be(matchSuccessResponse)
        verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
      }

      "for unsuccessful match, return error message" in {
        val inputJsonForUIB = Json.parse(
          s"""
             |{
             |  "businessType": "UIB",
             |  "uibCompany": {
             |    "uibBusinessName": "ACME",
             |    "uibCotaxAUTR": $noMatchUtr
             |  }
             |}
          """.stripMargin)
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())) thenReturn {
          Future.successful(HttpResponse(NOT_FOUND, responseJson = Some(matchFailureResponse)))
        }
        val result = TestEtmpConnector.lookup(inputJsonForUIB, saUserType, noMatchUtr.toString)
        await(result).json must be(matchFailureResponse)
        verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
      }

      "for server error, return error message" in {
        val inputJsonForUIB = Json.parse(
          s"""
             |{
             |  "businessType": "UIB",
             |  "uibCompany": {
             |    "uibBusinessName": "ACME",
             |    "uibCotaxAUTR": $matchUtr
             |  }
             |}
          """.stripMargin)
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())) thenReturn {
          Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, responseJson = Some(matchFailureResponse)))
        }
        val result = TestEtmpConnector.lookup(inputJsonForUIB, saUserType, matchUtr.toString)
        await(result).json must be(matchFailureResponse)
        verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
      }

    }

    "userType=org" must {

      val matchSuccessResponse = Json.parse(
        """
          |{
          |  "businessName":"ACME",
          |  "businessType":"Unincorporated body",
          |  "businessAddress":"23 High Street\nPark View\nThe Park\nGloucester\nGloucestershire\nABC 123",
          |  "businessTelephone":"201234567890",
          |  "businessEmail":"contact@acme.com"
          |}
        """.stripMargin)

      val matchFailureResponse = Json.parse( """{"error": "Sorry. Business details not found."}""")

      "for a successful match, return business details" in {
        val inputJsonForUIB = Json.parse(
          s"""{
              |  "businessType": "UIB",
              |  "uibCompany": {
              |    "uibBusinessName": "ACME",
              |    "uibCotaxAUTR": $matchUtr
              |  }
              |}
          """.stripMargin)
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())) thenReturn {
          Future.successful(HttpResponse(200, responseJson = Some(matchSuccessResponse)))
        }
        val result = TestEtmpConnector.lookup(inputJsonForUIB, orgUserType, matchUtr.toString)
        await(result).json must be(matchSuccessResponse)
        verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
      }

      "for unsuccessful match, return error message" in {
        val inputJsonForUIB = Json.parse(
          s"""
             |{
             |  "businessType": "UIB",
             |  "uibCompany": {
             |    "uibBusinessName": "ACME",
             |    "uibCotaxAUTR": $noMatchUtr
             |  }
             |}
          """.stripMargin)
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())) thenReturn {
          Future.successful(HttpResponse(NOT_FOUND, responseJson = Some(matchFailureResponse)))
        }
        val result = TestEtmpConnector.lookup(inputJsonForUIB, orgUserType, noMatchUtr.toString)
        await(result).json must be(matchFailureResponse)
        verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
      }

      "for server error, return error message" in {
        val inputJsonForUIB = Json.parse(
          s"""
             |{
             |  "businessType": "UIB",
             |  "uibCompany": {
             |    "uibBusinessName": "ACME",
             |    "uibCotaxAUTR": $matchUtr
             |  }
             |}
          """.stripMargin)
        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())) thenReturn {
          Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, responseJson = Some(matchFailureResponse)))
        }
        val result = TestEtmpConnector.lookup(inputJsonForUIB, orgUserType, matchUtr.toString)
        await(result).json must be(matchFailureResponse)
        verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
      }

    }

    "userType=Wrong type, throw an exception" in {

      val inputJsonForUIB = Json.parse(
        s"""
           |{
           |  "businessType": "UIB",
           |  "uibCompany": {
           |    "uibBusinessName": "ACME",
           |    "uibCotaxAUTR": $matchUtr
           |  }
           |}
        """.stripMargin)
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val thrown = the[RuntimeException] thrownBy await(TestEtmpConnector.lookup(inputJsonForUIB, "wrongType", matchUtr.toString))
      thrown.getMessage must be("Wrong user type!!")
      verify(mockWSHttp, times(0)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }
  }

}

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

package uk.gov.hmrc.connectors

import connectors.DefaultEtmpConnector
import metrics.ServiceMetrics
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import play.api.test.Injecting
import uk.gov.hmrc.domain.{SaUtr, SaUtrGenerator}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class EtmpConnectorSpec extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfter with Injecting {

  val mockWSHttp: HttpClient = mock(classOf[HttpClient])
  val mockServiceMetrics: ServiceMetrics = inject[ServiceMetrics]
  val mockAuditConnector: AuditConnector = inject[AuditConnector]

  implicit val hc: HeaderCarrier = HeaderCarrier()

  trait Setup {
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    val connector: DefaultEtmpConnector = new DefaultEtmpConnector(
      inject[ServicesConfig], mockWSHttp, mockAuditConnector, inject[ServiceMetrics]) {
      override val serviceUrl = ""
      override val indLookupURI: String = "registration/individual"
      override val orgLookupURI: String = "registration/organisation"
      override val http: HttpClient = mockWSHttp
      override val urlHeaderEnvironment: String = "env"
      override val urlHeaderAuthorization: String = "auth-token"
    }
  }

  before {
    reset(mockWSHttp)
  }

  val saUserType = "sa"
  val orgUserType = "org"
  val matchUtr: SaUtr = new SaUtrGenerator().nextSaUtr
  val noMatchUtr: SaUtr = new SaUtrGenerator().nextSaUtr

  "BusinessCustomerConnector" must {

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

      "for a successful match, return business details" in new Setup {

        val inputJsonForUIB: JsValue = Json.parse(
          s"""
             |{
             |  "businessType": "UIB",
             |  "uibCompany": {
             |    "uibBusinessName": "ACME",
             |    "uibCotaxAUTR": $matchUtr
             |  }
             |}
          """.stripMargin)



        when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn {
          Future.successful(HttpResponse.apply(200, matchSuccessResponse.toString()))
        }
        val result: Future[HttpResponse] = connector.lookup(inputJsonForUIB, saUserType, matchUtr.toString)
        await(result).json must be(matchSuccessResponse)
        verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](ArgumentMatchers.any(),
          ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
      }

      "for unsuccessful match, return error message" in new Setup {
        val inputJsonForUIB: JsValue = Json.parse(
          s"""
             |{
             |  "businessType": "UIB",
             |  "uibCompany": {
             |    "uibBusinessName": "ACME",
             |    "uibCotaxAUTR": $noMatchUtr
             |  }
             |}
          """.stripMargin)

        when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn {
          Future.successful(HttpResponse.apply(NOT_FOUND, matchFailureResponse.toString()))
        }
        val result: Future[HttpResponse] = connector.lookup(inputJsonForUIB, saUserType, noMatchUtr.toString)
        await(result).json must be(matchFailureResponse)
        verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](ArgumentMatchers.any(),
          ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
      }

      "for server error, return error message" in new Setup {
        val inputJsonForUIB: JsValue = Json.parse(
          s"""
             |{
             |  "businessType": "UIB",
             |  "uibCompany": {
             |    "uibBusinessName": "ACME",
             |    "uibCotaxAUTR": $matchUtr
             |  }
             |}
          """.stripMargin)

        when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn {
          Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, matchFailureResponse.toString()))
        }
        val result: Future[HttpResponse] = connector.lookup(inputJsonForUIB, saUserType, matchUtr.toString)
        await(result).json must be(matchFailureResponse)
        verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](ArgumentMatchers.any(),
          ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
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

      "for a successful match, return business details" in new Setup {
        val inputJsonForUIB: JsValue = Json.parse(
          s"""{
              |  "businessType": "UIB",
              |  "uibCompany": {
              |    "uibBusinessName": "ACME",
              |    "uibCotaxAUTR": $matchUtr
              |  }
              |}
          """.stripMargin)

        when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn {
          Future.successful(HttpResponse.apply(200, matchSuccessResponse.toString()))
        }
        val result: Future[HttpResponse] = connector.lookup(inputJsonForUIB, orgUserType, matchUtr.toString)
        await(result).json must be(matchSuccessResponse)
        verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](ArgumentMatchers.any(),
          ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
      }

      "for unsuccessful match, return error message" in new Setup {
        val inputJsonForUIB: JsValue = Json.parse(
          s"""
             |{
             |  "businessType": "UIB",
             |  "uibCompany": {
             |    "uibBusinessName": "ACME",
             |    "uibCotaxAUTR": $noMatchUtr
             |  }
             |}
          """.stripMargin)

        when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn {
          Future.successful(HttpResponse.apply(NOT_FOUND, matchFailureResponse.toString()))
        }
        val result: Future[HttpResponse] = connector.lookup(inputJsonForUIB, orgUserType, noMatchUtr.toString)
        await(result).json must be(matchFailureResponse)
        verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](ArgumentMatchers.any(),
          ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
      }

      "for server error, return error message" in new Setup {
        val inputJsonForUIB: JsValue = Json.parse(
          s"""
             |{
             |  "businessType": "UIB",
             |  "uibCompany": {
             |    "uibBusinessName": "ACME",
             |    "uibCotaxAUTR": $matchUtr
             |  }
             |}
          """.stripMargin)

        when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn {
          Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, matchFailureResponse.toString()))
        }
        val result: Future[HttpResponse] = connector.lookup(inputJsonForUIB, orgUserType, matchUtr.toString)
        await(result).json must be(matchFailureResponse)
        verify(mockWSHttp, times(1)).POST[JsValue, HttpResponse](ArgumentMatchers.any(),
          ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
      }

    }

    "userType=Wrong type, throw an exception" in new Setup {

      val inputJsonForUIB: JsValue = Json.parse(
        s"""
           |{
           |  "businessType": "UIB",
           |  "uibCompany": {
           |    "uibBusinessName": "ACME",
           |    "uibCotaxAUTR": $matchUtr
           |  }
           |}
        """.stripMargin)

      val thrown: RuntimeException = the[RuntimeException] thrownBy await(connector.lookup(inputJsonForUIB, "wrongType", matchUtr.toString))
      thrown.getMessage must be("[EtmpConnector][lookup] Incorrect user type")
      verify(mockWSHttp, times(0))
        .POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(),
          ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }
  }

}

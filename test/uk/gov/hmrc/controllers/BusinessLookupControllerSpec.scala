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

package uk.gov.hmrc.controllers

import connectors.EtmpConnector
import controllers.BusinessLookupController
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.test.{FakeRequest, Injecting}
import play.api.test.Helpers._
import uk.gov.hmrc.domain.SaUtrGenerator
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.Future

class BusinessLookupControllerSpec extends PlaySpec with GuiceOneServerPerSuite with Injecting {

  val mockDesConnector: EtmpConnector = mock(classOf[EtmpConnector])
  val mockControllerComponents: ControllerComponents = inject[ControllerComponents]

  trait Setup {
    class TestBusinessLookupController extends BackendController(mockControllerComponents) with BusinessLookupController {
      val desConnector: EtmpConnector = mockDesConnector
    }

    val controller = new TestBusinessLookupController()
  }


  val userType = "sa"
  val utr = new SaUtrGenerator().nextSaUtr.toString

  "BusinessLookupController" must {

    val atedRef = "ATED-123"
    "lookup" must {

      val matchSuccess = Json.parse(
        """{
          |  "businessName":"ACME",
          |  "businessType":"Unincorporated body",
          |  "businessAddress":"23 High Street\nPark View\nThe Park\nGloucester\nGloucestershire\nABC 123",
          |  "businessTelephone":"201234567890",
          |  "businessEmail":"contact@acme.com"
          |}
        """.stripMargin)

      val matchSuccessResponse = HttpResponse(OK, responseJson = Some(matchSuccess))

      val matchFailure = Json.parse(""" {"reason": "Resource not found"} """)

      val matchFailureResponse = HttpResponse(NOT_FOUND, responseJson = Some(matchFailure))

      val inputJsonForUIB = Json.parse(
        s"""
           |{
           |  "businessType": "UIB",
           |  "uibCompany": {
           |    "uibBusinessName": "ACME",
           |    "uibCotaxAUTR": $utr
           |  }
           |}
          """.stripMargin)

      "respond with OK" in new Setup {
        when(mockDesConnector.lookup(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(matchSuccessResponse))
        val result = controller.lookup(atedRef, utr, userType).apply(FakeRequest().withJsonBody(inputJsonForUIB))
        status(result) must be(OK)
      }

      "return Response as HttpResponse text/plain" in new Setup {
        when(mockDesConnector.lookup(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(matchSuccessResponse))
        val result = controller.lookup(atedRef, utr, userType).apply(FakeRequest().withJsonBody(inputJsonForUIB))
        contentType(result) must be(Some("text/plain"))
      }

      "for a successful match return Business Details" in new Setup {
        when(mockDesConnector.lookup(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(matchSuccessResponse))
        val result = controller.lookup(atedRef, utr, userType).apply(FakeRequest().withJsonBody(inputJsonForUIB))
        contentAsJson(result) must be(matchSuccessResponse.json)
      }

      "for an unsuccessful match return Not found" in new Setup {
        when(mockDesConnector.lookup(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(matchFailureResponse))
        val result = controller.lookup(atedRef, utr, userType).apply(FakeRequest().withJsonBody(inputJsonForUIB))
        status(result) must be(NOT_FOUND)
        contentAsJson(result) must be(matchFailureResponse.json)
      }

      "for a bad request, return BadRequest" in new Setup {
        val badRequestJson = Json.parse(""" { "reason" : "Bad Request" } """)
        when(mockDesConnector.lookup(Matchers.any(), Matchers.any(), Matchers.any())) thenReturn {
          Future.successful(HttpResponse(BAD_REQUEST, Some(badRequestJson)))
        }
        val result = controller.lookup(atedRef, utr, userType).apply(FakeRequest().withJsonBody(inputJsonForUIB))
        status(result) must be(BAD_REQUEST)
        contentAsJson(result) must be(badRequestJson)
      }

      "for service unavailable, return service unavailable" in new Setup {
        val serviceUnavailable = Json.parse(""" { "reason" : "Service unavailable" } """)
        when(mockDesConnector.lookup(Matchers.any(), Matchers.any(), Matchers.any())) thenReturn {
          Future.successful(HttpResponse(SERVICE_UNAVAILABLE, Some(serviceUnavailable)))
        }
        val result = controller.lookup(atedRef, utr, userType).apply(FakeRequest().withJsonBody(inputJsonForUIB))
        status(result) must be(SERVICE_UNAVAILABLE)
        contentAsJson(result) must be(serviceUnavailable)
      }

      "internal server error, return internal server error" in new Setup {
        val serverError = Json.parse(""" { "reason" : "Internal server error" } """)
        when(mockDesConnector.lookup(Matchers.any(), Matchers.any(), Matchers.any())) thenReturn {
          Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(serverError)))
        }
        val result = controller.lookup(atedRef, utr, userType).apply(FakeRequest().withJsonBody(inputJsonForUIB))
        status(result) must be(INTERNAL_SERVER_ERROR)
        contentAsJson(result) must be(serverError)
      }

    }

  }

}

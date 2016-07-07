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

package uk.gov.hmrc.controllers

import connectors.RegisterWithIdConnector
import controllers.RegisterWithIdController
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.HttpResponse

import scala.concurrent.Future

class RegisterWithIdControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val mockConnector: RegisterWithIdConnector = mock[RegisterWithIdConnector]

  object TestBusinessLookupController extends RegisterWithIdController {
    val connector = mockConnector
  }

  "RegisterWithIdController" must {

    "use the correct connector" in {
      RegisterWithIdController.connector must be(RegisterWithIdConnector)
    }

    val ggRef = "123"

    "lookup" must {

      val matchSuccess = Json.parse("""{ "response":"JSON"} """)

      val matchSuccessResponse = HttpResponse(OK, responseJson = Some(matchSuccess))

      val matchFailure = Json.parse(""" {"reason": "Resource not found"} """)

      val matchFailureResponse = HttpResponse(NOT_FOUND, responseJson = Some(matchFailure))

      val inputJson = Json.parse(""" { "input": "JSON" } """)

      "respond with OK" in {
        when(mockConnector.lookup(Matchers.any())).thenReturn(Future.successful(matchSuccessResponse))
        val result = TestBusinessLookupController.lookup(ggRef).apply(FakeRequest().withJsonBody(inputJson))
        status(result) must be(OK)
      }

      "return Response as HttpResponse text/plain" in {
        when(mockConnector.lookup(Matchers.any())).thenReturn(Future.successful(matchSuccessResponse))
        val result = TestBusinessLookupController.lookup(ggRef).apply(FakeRequest().withJsonBody(inputJson))
        contentType(result) must be(Some("text/plain"))
      }

      "for a successful match return Business Details" in {
        when(mockConnector.lookup(Matchers.any())).thenReturn(Future.successful(matchSuccessResponse))
        val result = TestBusinessLookupController.lookup(ggRef).apply(FakeRequest().withJsonBody(inputJson))
        contentAsJson(result) must be(matchSuccessResponse.json)
      }

      "for an unsuccessful match return Not found" in {
        when(mockConnector.lookup(Matchers.any())).thenReturn(Future.successful(matchFailureResponse))
        val result = TestBusinessLookupController.lookup(ggRef).apply(FakeRequest().withJsonBody(inputJson))
        status(result) must be(NOT_FOUND)
        contentAsJson(result) must be(matchFailureResponse.json)
      }

      "for a bad request, return BadRequest" in {
        val badRequestJson = Json.parse(""" { "reason" : "Bad Request" } """)
        when(mockConnector.lookup(Matchers.any())) thenReturn {
          Future.successful(HttpResponse(BAD_REQUEST, Some(badRequestJson)))
        }
        val result = TestBusinessLookupController.lookup(ggRef).apply(FakeRequest().withJsonBody(inputJson))
        status(result) must be(BAD_REQUEST)
        contentAsJson(result) must be(badRequestJson)
      }

      "for service unavailable, return service unavailable" in {
        val serviceUnavailable = Json.parse(""" { "reason" : "Service unavailable" } """)
        when(mockConnector.lookup(Matchers.any())) thenReturn {
          Future.successful(HttpResponse(SERVICE_UNAVAILABLE, Some(serviceUnavailable)))
        }
        val result = TestBusinessLookupController.lookup(ggRef).apply(FakeRequest().withJsonBody(inputJson))
        status(result) must be(SERVICE_UNAVAILABLE)
        contentAsJson(result) must be(serviceUnavailable)
      }

      "internal server error, return internal server error" in {
        val serverError = Json.parse(""" { "reason" : "Internal server error" } """)
        when(mockConnector.lookup(Matchers.any())) thenReturn {
          Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(serverError)))
        }
        val result = TestBusinessLookupController.lookup(ggRef).apply(FakeRequest().withJsonBody(inputJson))
        status(result) must be(INTERNAL_SERVER_ERROR)
        contentAsJson(result) must be(serverError)
      }

      "for empty request payload, return bad request" in {
        val result = TestBusinessLookupController.lookup(ggRef).apply(FakeRequest())
        status(result) must be(BAD_REQUEST)
        contentAsString(result) must be("")
      }
    }
  }
}
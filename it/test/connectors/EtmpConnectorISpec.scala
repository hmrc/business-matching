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

import com.github.tomakehurst.wiremock.client.WireMock._
import helpers.IntegrationSpec
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.domain.{SaUtr, SaUtrGenerator}
import uk.gov.hmrc.http._

import scala.concurrent.Future

class EtmpConnectorISpec extends IntegrationSpec {

  val connector: EtmpConnector = inject[EtmpConnector]

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

      val matchFailureResponse = Json.parse("""{"error": "Sorry. Business details not found."}""")

      "for a successful match, return business details" in {

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

        stubFor(
          post(urlEqualTo(s"/registration/individual/${matchUtr.toString()}"))
            willReturn aResponse()
            .withStatus(OK)
            .withBody(matchSuccessResponse.toString())
        )

        val result: Future[HttpResponse] = connector.lookup(inputJsonForUIB, saUserType, matchUtr.toString)
        await(result).json must be(matchSuccessResponse)
        wireMockServer.verify(1, postRequestedFor(urlMatching(s"/registration/individual/${matchUtr.toString()}")))
      }

      "for unsuccessful match, return error message" in {
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

        stubFor(
          post(urlEqualTo(s"/registration/individual/${noMatchUtr.toString()}"))
            willReturn aResponse()
            .withStatus(NOT_FOUND)
            .withBody(matchFailureResponse.toString())
        )

        val result: Future[HttpResponse] = connector.lookup(inputJsonForUIB, saUserType, noMatchUtr.toString)
        await(result).json must be(matchFailureResponse)
        wireMockServer.verify(1, postRequestedFor(urlMatching(s"/registration/individual/${noMatchUtr.toString()}")))
      }

      "for server error, return error message" in {
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

        stubFor(
          post(urlEqualTo(s"/registration/individual/${matchUtr.toString()}"))
            willReturn aResponse()
            .withStatus(INTERNAL_SERVER_ERROR)
            .withBody(matchFailureResponse.toString())
        )

        val result: Future[HttpResponse] = connector.lookup(inputJsonForUIB, saUserType, matchUtr.toString)
        await(result).json must be(matchFailureResponse)
        wireMockServer.verify(1, postRequestedFor(urlMatching(s"/registration/individual/${matchUtr.toString()}")))
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

      val matchFailureResponse = Json.parse("""{"error": "Sorry. Business details not found."}""")

      "for a successful match, return business details" in {
        val inputJsonForUIB: JsValue = Json.parse(
          s"""{
             |  "businessType": "UIB",
             |  "uibCompany": {
             |    "uibBusinessName": "ACME",
             |    "uibCotaxAUTR": $matchUtr
             |  }
             |}
          """.stripMargin)

        stubFor(
          post(urlEqualTo(s"/registration/organisation/${matchUtr.toString()}"))
            willReturn aResponse()
            .withStatus(OK)
            .withBody(matchSuccessResponse.toString())
        )

        val result: Future[HttpResponse] = connector.lookup(inputJsonForUIB, orgUserType, matchUtr.toString)
        await(result).json must be(matchSuccessResponse)
        wireMockServer.verify(1, postRequestedFor(urlMatching(s"/registration/organisation/${matchUtr.toString()}")))
      }

      "for unsuccessful match, return error message" in {
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

        stubFor(
          post(urlEqualTo(s"/registration/organisation/${noMatchUtr.toString()}"))
            willReturn aResponse()
            .withStatus(NOT_FOUND)
            .withBody(matchFailureResponse.toString())
        )

        val result: Future[HttpResponse] = connector.lookup(inputJsonForUIB, orgUserType, noMatchUtr.toString)
        await(result).json must be(matchFailureResponse)
        wireMockServer.verify(1, postRequestedFor(urlMatching(s"/registration/organisation/${noMatchUtr.toString()}")))
      }

      "for server error, return error message" in {
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

        stubFor(
          post(urlEqualTo(s"/registration/organisation/${matchUtr.toString()}"))
            willReturn aResponse()
            .withStatus(INTERNAL_SERVER_ERROR)
            .withBody(matchFailureResponse.toString())
        )

        val result: Future[HttpResponse] = connector.lookup(inputJsonForUIB, orgUserType, matchUtr.toString)
        await(result).json must be(matchFailureResponse)
        wireMockServer.verify(1, postRequestedFor(urlMatching(s"/registration/organisation/${matchUtr.toString()}")))
      }

    }

    "userType=Wrong type, throw an exception" in {

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
      wireMockServer.verify(0, postRequestedFor(urlMatching(s"/registration/individual/${matchUtr.toString()}")))
      wireMockServer.verify(0, postRequestedFor(urlMatching(s"/registration/organisation/${matchUtr.toString()}")))
    }

  }
}



/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers

import helpers.IntegrationSpec
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import com.github.tomakehurst.wiremock.client.WireMock._

class BusinessLookupControllerISpec extends IntegrationSpec {

  Map(
    "org"   -> "orgName",
    "sa"    -> "userName",
    "agent" -> "agentName"
  ) foreach { case (userType, name) =>
    s"/$userType/$name/business-matching/business-lookup/utr/$userType" should {
      "lookup business details" when {
        "business details are available" in {
          val utr = "2131231231"
          val desType = if (userType == "sa") "sa" else "org"
          val uriType = if (desType == "org") "organisation" else "individual"

          stubFor(post(urlMatching(s"/registration/$uriType/$utr"))
            .willReturn(
              aResponse()
                .withStatus(200)
                .withBody(
                  s"""{
                     |"isAnIndividual" : true,
                     |"individual": {
                     |  "firstName": "First",
                     |  "lastName" : "Last"
                     |}
                     |}""".stripMargin
                )
            )
          )

          val result: WSResponse = await(hitApplicationEndpoint(s"/$userType/$name/business-matching/business-lookup/$utr/$desType")
            .post(Json.parse(s"""
             |{
             |  "businessType": "UIB",
             |  "uibCompany": {
             |    "uibBusinessName": "ACME",
             |    "uibCotaxAUTR": $utr
             |  }
             |}
            """.stripMargin)))

          result.status mustBe 200
        }
      }
    }
  }
}
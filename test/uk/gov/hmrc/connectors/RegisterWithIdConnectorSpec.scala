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

import com.codahale.metrics.Timer.Context
import connectors.RegisterWithIdConnector
import metrics.Metrics
import metrics.MetricsEnum.REGISTER_WITH_ID_MATCH
import connectors.RegisterWithIdConnector._
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, Matchers}
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeApplication
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.logging.Authorization
import uk.gov.hmrc.play.http.ws._
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpPost, HttpResponse}

import scala.concurrent.Future

class RegisterWithIdConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfter {

  val serviceHost = "someHost"
  val servicePort = "65"
  val serviceContext = "registrations"
  val serviceBearerToken = "1One2Two"

  val postUrl = s"http://$serviceHost:$servicePort/$serviceContext/registerwithid/1.0.0"

  class MockHttp extends WSGet with WSPost {
    override val hooks = NoneRequired
  }

  val mockWSHttp = mock[MockHttp]
  val mockMetrics = mock[Metrics]
  private val someJsValue: Some[JsValue] = Some(Json.parse("""{"input": "success"}"""))
  val okApiResponse = HttpResponse(OK, someJsValue)
  val notOkAPIResponse = HttpResponse(1, someJsValue)
  val mockTimerContext = mock[Context]
  val mockJs = mock[JsValue]

  before {
    reset(mockWSHttp, mockMetrics, mockTimerContext)
    when(mockMetrics.startTimer(REGISTER_WITH_ID_MATCH)).thenReturn(mockTimerContext)
  }

  object TestRegisterWithIdConnector extends RegisterWithIdConnector {
    val http: HttpGet with HttpPost = mockWSHttp
    val metrics = mockMetrics
    val serviceURL = s"http://$serviceHost:$servicePort"
    val baseURI = serviceContext
    val bearerToken = serviceBearerToken
    val version = "1.0.0"
  }

  "RegisterWithIdConnector" must {

    "return correct response when posted to correct url with correct payload and AUTH token" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any[String], Matchers.eq(mockJs), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(okApiResponse))
      val result = TestRegisterWithIdConnector.lookup(mockJs)
      await(result) must be(okApiResponse)

      val ac: ArgumentCaptor[HeaderCarrier] = ArgumentCaptor.forClass(classOf[HeaderCarrier])
      verify(mockWSHttp).POST[JsValue, HttpResponse](Matchers.eq(postUrl), Matchers.eq(mockJs), Matchers.any())(Matchers.any(), Matchers.any(), ac.capture())
      ac.getValue.authorization.get mustEqual Authorization(s"Bearer $serviceBearerToken")
    }

    "increase the SuccessCounter when response is OK" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any[String], Matchers.any[JsValue], Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(okApiResponse))
      await(TestRegisterWithIdConnector.lookup(mockJs))

      verify(mockTimerContext).stop()
      verify(mockMetrics).incrementSuccessCounter(REGISTER_WITH_ID_MATCH)

    }

    "increase the FailedCounter when response is not OK" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any[String], Matchers.eq(mockJs), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(notOkAPIResponse))
      val result = TestRegisterWithIdConnector.lookup(mockJs)
      await(result) must be(notOkAPIResponse)

      verify(mockTimerContext).stop()
      verify(mockMetrics).incrementFailedCounter(REGISTER_WITH_ID_MATCH)
    }

  }

}

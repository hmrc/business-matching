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

package helpers

import java.util.concurrent.TimeUnit

import org.scalatest.Assertion
import play.api.libs.ws.WSResponse
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

import scala.concurrent.Future

trait AssertionHelpers extends FutureAwaits with DefaultAwaitTimeout {

  def assertWsResponse(response: Future[WSResponse])(assertions: WSResponse => Assertion): Assertion = {
    assertions(await(response))
  }

  def awaitAndAssert[T](methodUnderTest: => Future[T])(assertions: T => Assertion): Assertion = {
    assertions(await(methodUnderTest,60, TimeUnit.SECONDS))
  }

  def assertResults[T](methodUnderTest: => T)(assertions: T => Assertion): Assertion = {
    assertions(methodUnderTest)
  }
}

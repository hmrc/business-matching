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


package helpers

import helpers.application.IntegrationApplication
import helpers.wiremock.WireMockSetup
import org.scalatest._
import org.scalatestplus.play.PlaySpec
import play.api.libs.ws.WSRequest
import uk.gov.hmrc.http.HeaderCarrier

trait IntegrationSpec
  extends PlaySpec
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with IntegrationApplication
    with WireMockSetup
    with AssertionHelpers {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWmServer()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    resetWmServer()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    stopWmServer()
  }

  def hitApplicationEndpoint(url: String): WSRequest = {
    val appendSlash = if(url.startsWith("/")) url else s"/$url"
    ws.url(s"$testAppUrl$appendSlash")
  }
}

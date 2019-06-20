
package helpers.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import play.api.Logger

trait WireMockSetup {
  self: WireMockConfig =>

  private val wireMockServer = new WireMockServer(wireMockConfig().port(wireMockPort))

  protected def startWmServer(): Unit = {
    Logger.info(s"[startWmServer] - Starting wiremock server on port $wireMockPort")
    wireMockServer.start()
    WireMock.configureFor(wireMockHost, wireMockPort)
  }

  protected def stopWmServer(): Unit = {
    Logger.info(s"[startWmServer] - Integration test complete; stopping wiremock server")
    wireMockServer.stop()
  }

  protected def resetWmServer(): Unit = {
    Logger.info("[resetWmServer] - Resetting wiremock server for new tests")
    wireMockServer.resetAll()
    WireMock.reset()
  }
}

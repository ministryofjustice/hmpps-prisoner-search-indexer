package uk.gov.justice.digital.hmpps.prisonersearch.indexer.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.google.gson.Gson
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.prisonersearch.indexer.integration.PrisonerBuilder
import uk.gov.justice.digital.hmpps.prisonersearch.indexer.services.PrisonerNumberPage

class PrisonApiMockServer : WireMockServer(8093) {
  fun stubHealthPing(status: Int) {
    stubFor(
      WireMock.get("/health/ping").willReturn(
        WireMock.aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(if (status == 200) "pong" else "some error")
          .withStatus(status),
      ),
    )
  }
  fun stubOffenders(vararg prisoners: PrisonerBuilder) {
    val prisonerNumbers = prisoners.map { it.prisonerNumber }
    stubFor(
      WireMock.get(WireMock.urlPathEqualTo("/api/prisoners/prisoner-numbers"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(Gson().toJson(PrisonerNumberPage(prisonerNumbers, prisonerNumbers.size.toLong()))),
        ),
    )

    prisoners.forEach {
      stubFor(
        WireMock.get(WireMock.urlEqualTo("/api/offenders/${it.prisonerNumber}"))
          .willReturn(
            WireMock.aResponse()
              .withHeader("Content-Type", "application/json")
              .withBody(it.toOffenderBooking()),
          ),
      )
      stubFor(
        WireMock.get(WireMock.urlPathEqualTo("/iep/reviews/booking/${it.bookingId}"))
          .willReturn(
            WireMock.aResponse()
              .withHeader("Content-Type", "application/json")
              .withBody(it.toIncentiveLevel()),
          ),
      )
    }
  }
}

class PrisonApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val prisonApi = PrisonApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext): Unit = prisonApi.start()
  override fun beforeEach(context: ExtensionContext): Unit = prisonApi.resetAll()
  override fun afterAll(context: ExtensionContext): Unit = prisonApi.stop()
}

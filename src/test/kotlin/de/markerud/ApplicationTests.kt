package de.markerud

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.mockserver.model.HttpRequest.request
import org.springframework.http.HttpStatus.OK
import java.time.Duration

class ApplicationTests : AbstractTestBase() {

    @Test
    fun `access logs and logbook logs are enabled`() {
        BACKEND.respondToAnyRequest(OK)
        ZIPKIN.respondToAnyRequest(OK)

        webClient
            .get().uri("/something")
            .exchange()
            .expectStatus().isOk

        await()
            .atMost(Duration.ofSeconds(1))
            .untilAsserted {
                assertThat(appender.list)
                    .filteredOn { event -> event.loggerName == accessLogLogger.name }
                    .hasSize(1)
                assertThat(appender.list)
                    .filteredOn { event -> event.loggerName == logbookLogger.name }
                    .hasSize(4)
            }

        BACKEND.verify(
            request()
                .withMethod("GET")
                .withPath("/something")
        )
    }

    @Test
    fun `access log and logbook logs for excluded endpoint are disabled`() {
        BACKEND.respondToAnyRequest(OK)
        ZIPKIN.respondToAnyRequest(OK)

        webClient
            .get().uri("/anonymous")
            .exchange()
            .expectStatus().isOk

        assertThat(appender.list)
            .filteredOn { event -> event.loggerName.equals(accessLogLogger.name) }
            .isEmpty()
        assertThat(appender.list)
            .filteredOn { event -> event.loggerName == logbookLogger.name }
            .isEmpty()

        BACKEND.verify(
            request()
                .withMethod("GET")
                .withPath("/anonymous")
        )
    }

}

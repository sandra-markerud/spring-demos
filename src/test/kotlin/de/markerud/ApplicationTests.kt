package de.markerud

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import de.markerud.ApplicationTests.Companion.BACKEND_PORT
import de.markerud.ApplicationTests.Companion.ZIPKIN_PORT
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockserver.client.MockServerClient
import org.mockserver.integration.ClientAndServer
import org.mockserver.junit.jupiter.MockServerSettings
import org.mockserver.matchers.Times
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.slf4j.LoggerFactory
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.OK
import org.springframework.test.web.reactive.server.WebTestClient
import org.zalando.logbook.Logbook
import java.time.Duration

@Suppress("SpringBootApplicationProperties")
@MockServerSettings
@AutoConfigureObservability
@SpringBootTest(
    webEnvironment = RANDOM_PORT, properties = [
        "BACKEND=http://localhost:$BACKEND_PORT",
        "ZIPKIN=http://localhost:$ZIPKIN_PORT/api/v2/spans",
        "management.tracing.sampling.probability=1.0",
        "logging.level.org.mockserver.log=WARN",
    ]
)
class ApplicationTests {

    lateinit var appender: ListAppender<ILoggingEvent>

    val accessLogLogger = LoggerFactory.getLogger(ACCESS_LOG_LOGGER_NAME) as Logger
    val logbookLogger = LoggerFactory.getLogger(Logbook::class.java) as Logger

    @LocalServerPort
    val serverPort = -1

    val webClient: WebTestClient by lazy {
        WebTestClient.bindToServer()
            .baseUrl("http://localhost:$serverPort")
            .build()
    }

    @BeforeEach
    fun setup() {
        appender = ListAppender()
        appender.start()
        accessLogLogger.addAppender(appender)
        logbookLogger.addAppender(appender)
    }

    @AfterEach
    fun tearDownLogAppender() {
        accessLogLogger.detachAppender(appender)
        logbookLogger.detachAppender(appender)
    }

    companion object {
        private const val ACCESS_LOG_LOGGER_NAME = "reactor.netty.http.server.AccessLog"

        const val BACKEND_PORT = 12000
        const val ZIPKIN_PORT = 13000

        val BACKEND: ClientAndServer = ClientAndServer.startClientAndServer(BACKEND_PORT)
        val ZIPKIN: ClientAndServer = ClientAndServer.startClientAndServer(ZIPKIN_PORT)
    }


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

fun MockServerClient.whenever(request: HttpRequest, times: Times = Times.unlimited()) =
    this.`when`(request, times)!!

fun MockServerClient.respondToAnyRequest(status: HttpStatus) {
    whenever(request()).respond(response().withStatusCode(status.value()))
}

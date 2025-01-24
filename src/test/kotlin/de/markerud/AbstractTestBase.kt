package de.markerud

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import de.markerud.AbstractTestBase.Companion.BACKEND_PORT
import de.markerud.AbstractTestBase.Companion.ZIPKIN_PORT
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
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
import org.springframework.test.web.reactive.server.WebTestClient
import org.zalando.logbook.Logbook

@Suppress("SpringBootApplicationProperties")
@MockServerSettings
@AutoConfigureObservability
@SpringBootTest(
    webEnvironment = RANDOM_PORT, properties = [
        "BACKEND=http://localhost:$BACKEND_PORT",
        "ZIPKIN=http://localhost:$ZIPKIN_PORT/api/v2/spans",
        "management.tracing.sampling.probability=1.0",
        "logging.level.org.mockserver.log=WARN"
    ]
)
abstract class AbstractTestBase {

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

}

fun MockServerClient.whenever(request: HttpRequest, times: Times = Times.unlimited()) =
    this.`when`(request, times)!!

fun MockServerClient.respondToAnyRequest(status: HttpStatus) {
    whenever(request()).respond(response().withStatusCode(status.value()))
}

fun MockServerClient.verifySampled(): MockServerClient = this.verify(request().withPath("/api/v2/spans").withMethod("POST"))

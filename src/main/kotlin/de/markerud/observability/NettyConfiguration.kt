package de.markerud.observability

import de.markerud.observability.NettyConfiguration.NettyRole.CLIENT
import de.markerud.observability.NettyConfiguration.NettyRole.SERVER
import io.micrometer.context.ContextSnapshotFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.web.reactive.function.client.ReactorNettyHttpClientMapper
import org.springframework.boot.web.embedded.netty.NettyServerCustomizer
import org.springframework.cloud.gateway.config.HttpClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.zalando.logbook.Logbook
import org.zalando.logbook.netty.LogbookClientHandler
import org.zalando.logbook.netty.LogbookServerHandler
import reactor.netty.Connection
import reactor.netty.http.client.HttpClient
import reactor.netty.http.server.logging.AccessLogFactory
import java.util.function.Consumer

@Configuration
class NettyConfiguration(
    private val contextSnapshotFactory: ContextSnapshotFactory,
    @Value("\${logbook.filter.enabled:false}") private val logbookEnabled: Boolean,
    private val logbook: Logbook
) {

    private val excludedPathPrefixes = setOf("/anonymous")

    /**
     * Decorates the global netty [HttpClient] with all relevant handlers (e.g. for tracing, logbook).
     */
    @Bean
    fun reactorNettyHttpClientMapper() = ReactorNettyHttpClientMapper {
        it.doOnConnected(addHandlers(CLIENT))
    }

    /**
     * Decorates the global netty [HttpClient] with all relevant handlers (e.g. for tracing, logbook).
     */
    @Bean
    fun httpClientCustomizer(
        contextSnapshotFactory: ContextSnapshotFactory
    ): HttpClientCustomizer = HttpClientCustomizer {
        it.doOnConnected(addHandlers(CLIENT))
    }

    /**
     * Customizes the netty server to
     *  - enable the netty access-log
     *  - enable metrics to be collected and registered in Micrometer's globalRegistry
     *  - add all relevant handlers (e.g. for tracing, logbook)
     */
    @Bean
    fun defaultNettyServerCustomizer(): NettyServerCustomizer =
        NettyServerCustomizer { server ->
            server
                .accessLog(true, accessLogFactory())
                .metrics(true) { uriTagValue: String -> uriTagValue }
                .doOnConnection(addHandlers(SERVER))
        }

    /**
     * Creates an AccessLogFactory that defines a filter for access logs.
     * This filter checks if the request URI matches any of the excluded path prefixes.
     * If the URI of the request starts with any of the excluded prefixes, the request is
     * not logged; otherwise, it is logged.
     */
    private fun accessLogFactory(): AccessLogFactory =
        AccessLogFactory.createFilter { provider ->
            provider.uri()
                ?.let { chars ->
                    val uri = chars.toString()
                    excludedPathPrefixes.none { prefix -> uri.startsWith(prefix) }
                }
                ?: false
        }

    private fun addHandlers(role: NettyRole) = Consumer<Connection> { connection: Connection ->
        connection.addHandlerFirst(TracingChannelInboundHandler(contextSnapshotFactory))

        if (logbookEnabled) {
            connection.addHandlerLast(
                when (role) {
                    CLIENT -> LogbookClientHandler(logbook)
                    SERVER -> LogbookServerHandler(logbook)
                }
            )

        }

        connection.addHandlerLast(TracingChannelOutboundHandler(contextSnapshotFactory))
    }

    private enum class NettyRole {
        CLIENT, SERVER
    }

}

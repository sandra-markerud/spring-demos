package de.markerud.observability

import io.micrometer.tracing.TraceContext
import io.micrometer.tracing.Tracer
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.zalando.logbook.CorrelationId
import org.zalando.logbook.core.DefaultCorrelationId

private fun TraceContext.toCorrelationId() = "${this.traceId()}/${this.spanId()}"

@Configuration
@ConditionalOnProperty(value = ["logbook.filter.enabled"], havingValue = "true")
class LogbookConfiguration {

    /**
     * Logbook CorrelationId reusing Micrometer's TraceId/SpanId.
     * As the logbook filter has lower precedence than the tracing filter, we can assume
     * that the tracing context is already set. In case the tracing context is not yet
     * set, e.g. for server-to-server communication, the [DefaultCorrelationId] is used.
     */
    @Bean
    fun tracingCorrelationId(tracer: Tracer): CorrelationId = CorrelationId { request ->
        tracer.currentSpan()
            ?.context()?.toCorrelationId()
            ?: DefaultCorrelationId().generate(request)
    }

}

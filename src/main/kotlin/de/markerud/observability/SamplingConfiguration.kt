package de.markerud.observability

import brave.Tracing
import brave.http.HttpRequest
import brave.http.HttpRequestMatchers.pathStartsWith
import brave.http.HttpRuleSampler
import brave.http.HttpTracing
import brave.sampler.Matcher
import brave.sampler.Sampler.ALWAYS_SAMPLE
import brave.sampler.Sampler.NEVER_SAMPLE
import io.micrometer.observation.ObservationPredicate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext

@Configuration
class SamplingConfiguration {

//    @Bean
//    fun doNotObserveActuator(): ObservationPredicate = ObservationPredicate { name, context ->
//        if ("http.server.requests" == name && context is ServerRequestObservationContext) {
//            !context.carrier.path.value().startsWith("/actuator")
//        } else true
//    }

    @Bean
    fun httpTracing(tracing: Tracing): HttpTracing {
        return HttpTracing.newBuilder(tracing).serverSampler(customHttpSampler()).build()
    }

//    @Bean
//    fun httpTracingCustomizer(): HttpTracingCustomizer = HttpTracingCustomizer { builder ->
//        builder.serverSampler(customHttpSampler())
//    }

    fun customHttpSampler(): HttpRuleSampler = HttpRuleSampler.newBuilder()
        .putRule(pathStartsWith("/actuator"), NEVER_SAMPLE)
        .putRule(pathStartsWith("/anonymous"), NEVER_SAMPLE)
        .putRule(containsHeaderValue("foo-name", "foo-value"), ALWAYS_SAMPLE)
        .putRule(containsHeaderValue("bar-name", "bar-value"), ALWAYS_SAMPLE)
        .build()

    private fun containsHeaderValue(name: String, value: String): Matcher<HttpRequest> =
        Matcher<HttpRequest> { request: HttpRequest ->
            value == request.header(name)
        }

}

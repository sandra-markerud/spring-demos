package de.markerud

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.zalando.logbook.autoconfigure.webflux.LogbookWebFluxAutoConfiguration

@ConfigurationPropertiesScan
@SpringBootApplication(
    exclude = [
        LogbookWebFluxAutoConfiguration::class,
    ]
)
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}

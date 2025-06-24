package de.markerud.config

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties("some.props")
data class SomeProps(
    @NotNull val enabled: Boolean,
    @NotBlank val someValue: String,
    @NotNull val nestedProps: NestedProps
) {

    data class NestedProps(
        @NotNull val nestedEnabled: Boolean,
        @NotBlank val nestedValue: String,
        val nestedCollection: List<SomeType>
    ) {

        data class SomeType(
            @NotBlank val someId: String,
            @NotBlank val someName: String
        )
    }

}

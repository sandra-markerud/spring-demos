package de.markerud

import de.markerud.config.SomeProps
import de.markerud.config.SomeProps.NestedProps.SomeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables
import uk.org.webcompere.systemstubs.jupiter.SystemStub
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension

@SpringBootTest
@ExtendWith(SystemStubsExtension::class)
class EnvSyntaxTest {

    @Autowired
    private lateinit var props: SomeProps

    @Test
    fun `injection from environment variables works`() {
        assertThat(props.enabled).isEqualTo(true)
        assertThat(props.someValue).isEqualTo("some-value")
        assertThat(props.nestedProps.nestedEnabled).isEqualTo(true)
        assertThat(props.nestedProps.nestedValue).isEqualTo("nested-value")
        assertThat(props.nestedProps.nestedCollection).containsExactlyInAnyOrder(
            SomeType("first-id", "first-name"),
            SomeType("second-id", "second-name"),
            SomeType("third-id", "third-name")
        )
    }

    companion object {
        @SystemStub
        @Suppress("unused")
        val environmentVariables = EnvironmentVariables(
            "SOME_PROPS_ENABLED", "true",
            "SOME_PROPS_SOME_VALUE", "some-value",
            "SOME_PROPS_NESTED_PROPS_NESTED_ENABLED", "true",
            "SOME_PROPS_NESTED_PROPS_NESTED_VALUE", "nested-value",
            "SOME_PROPS_NESTED_PROPS_NESTED_COLLECTION_0_SOME_ID", "first-id",
            "SOME_PROPS_NESTED_PROPS_NESTED_COLLECTION_0_SOME_NAME", "first-name",
            "SOME_PROPS_NESTED_PROPS_NESTED_COLLECTION_1_SOME_ID", "second-id",
            "SOME_PROPS_NESTED_PROPS_NESTED_COLLECTION_1_SOME_NAME", "second-name",
            "SOME_PROPS_NESTED_PROPS_NESTED_COLLECTION_2_SOME_ID", "third-id",
            "SOME_PROPS_NESTED_PROPS_NESTED_COLLECTION_2_SOME_NAME", "third-name",
        )
    }

}

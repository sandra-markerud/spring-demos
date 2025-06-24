package de.markerud

import de.markerud.config.SomeProps
import de.markerud.config.SomeProps.NestedProps.SomeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    properties = [
        "some.props.enabled=true",
        "some.props.some-value=some-value",
        "some.props.nested-props.nested-enabled=true",
        "some.props.nested-props.nested-value=nested-value",
        "some.props.nested-props.nested-collection[0].some-id=first-id",
        "some.props.nested-props.nested-collection[0].some-name=first-name",
        "some.props.nested-props.nested-collection[1].some-id=second-id",
        "some.props.nested-props.nested-collection[1].some-name=second-name",
        "some.props.nested-props.nested-collection[2].some-id=third-id",
        "some.props.nested-props.nested-collection[2].some-name=third-name",
    ]
)
class PropertiesTest {

    @Autowired
    private lateinit var props: SomeProps

    @Test
    fun `injection from properties works`() {
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

}

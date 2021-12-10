package recce.server

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@MicronautTest(
    environments = arrayOf("test-integration"),
    propertySources = arrayOf("classpath:config/application-test-dataset.yml"),
)
internal class RecConfigurationTest {

    @Inject
    lateinit var config: RecConfiguration

    @Test
    fun `should parse dataset configuration from yaml`() {
        assertThat(config.datasets)
            .hasSize(1)
            .hasEntrySatisfying("test-dataset") {
                assertThat(it.id).isEqualTo("test-dataset")
                assertThat(it.source.datasourceRef).isEqualTo("source-h2")
                assertThat(it.source.query).contains("select name as MigrationKey")
                assertThat(it.source.dbOperations).isNotNull
                assertThat(it.target.datasourceRef).isEqualTo("target-h2")
                assertThat(it.target.query).contains("select name as MigrationKey")
                assertThat(it.target.dbOperations).isNotNull
            }
    }
}

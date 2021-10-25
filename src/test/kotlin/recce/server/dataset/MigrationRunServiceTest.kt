package recce.server.dataset

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant

internal class MigrationRunServiceTest {

    private val startedRun = MigrationRun(1, datasetId, Instant.now())

    @Test
    fun `should return start results`() {

        val runRepository = mock<MigrationRunRepository> {
            on { save(any()) } doReturn Mono.just(startedRun)
        }

        val eventualRun = MigrationRunService(runRepository).start(datasetId)

        StepVerifier.create(eventualRun)
            .expectNext(startedRun)
            .verifyComplete()
    }

    @Test
    fun `complete should set completed time`() {
        val runRepository = mock<MigrationRunRepository> {
            on { update(any()) } doReturn Mono.just(startedRun)
        }

        StepVerifier.create(MigrationRunService(runRepository).complete(startedRun))
            .assertNext {
                assertThat(it.completedTime).isAfterOrEqualTo(it.createdTime)
            }
            .verifyComplete()
    }

    companion object {
        private const val datasetId = "my-dataset"
    }
}

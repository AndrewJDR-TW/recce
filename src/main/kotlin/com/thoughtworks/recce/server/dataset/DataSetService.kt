package com.thoughtworks.recce.server.dataset

import com.thoughtworks.recce.server.config.DataLoadDefinition
import com.thoughtworks.recce.server.config.ReconciliationConfiguration
import io.micronaut.discovery.event.ServiceReadyEvent
import io.micronaut.runtime.event.annotation.EventListener
import io.micronaut.scheduling.annotation.Async
import jakarta.inject.Inject
import jakarta.inject.Singleton
import mu.KotlinLogging
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

@Singleton
open class DataSetService(
    @Inject private val config: ReconciliationConfiguration,
    private val runRepository: MigrationRunRepository,
    private val recordRepository: MigrationRecordRepository
) {
    fun start(dataSetId: String): Mono<MigrationRun> {

        val source = config.datasets[dataSetId]?.source
            ?: throw IllegalArgumentException("[$dataSetId] not found!")

        logger.info { "Starting reconciliation run for [$dataSetId]..." }

        val savedMigration = runRepository
            .save(MigrationRun(dataSetId))
            .doOnNext { logger.info { "Starting reconciliation run for $it}..." } }
            .cache()

        return streamFromSource(source, savedMigration)
            .count()
            .zipWith(savedMigration)
            .map { it.t2.apply { results = DataSetResults(it.t1) } }
    }

    private fun streamFromSource(
        source: DataLoadDefinition,
        run: Mono<MigrationRun>
    ) = Mono.from(source.dbOperations.connectionFactory().create())
        .flatMapMany { it.createStatement(source.query).execute() }
        .flatMap { result -> result.map(::toHashedRow) }
        .zipWith(run.repeat())
        .map { tuple ->
            MigrationRecord(MigrationRecordKey(tuple.t2.id!!, tuple.t1.migrationKey)).apply {
                sourceData = tuple.t1.hashedValue
            }
        }
        .flatMap { record -> recordRepository.save(record) }

    @EventListener
    @Async
    open fun doOnStart(event: ServiceReadyEvent): Mono<MigrationRun> {
        return start("test-dataset")
            .doOnEach { logger.info { it.toString() } }
    }
}

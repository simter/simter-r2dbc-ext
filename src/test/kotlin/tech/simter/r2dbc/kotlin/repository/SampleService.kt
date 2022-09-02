package tech.simter.r2dbc.kotlin.repository

import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import tech.simter.r2dbc.kotlin.insert

@Service
class SampleService(
  private val databaseClient: DatabaseClient,
  private val entityTemplate: R2dbcEntityTemplate,
  private val otherService: OtherService,
) {
  fun insertFailedByUniqueWithoutTransaction(): Mono<Sample> {
    return entityTemplate.insert(SAMPLE)
      .then(entityTemplate.insert(SAMPLE))
  }

  @Transactional(readOnly = false)
  fun insertFailedByUniqueWithTransaction(): Mono<Sample> {
    return entityTemplate.insert(SAMPLE)
      .then(entityTemplate.insert(SAMPLE))
  }

  @Transactional(readOnly = false)
  fun insertWithOtherServiceTransaction(sample: Sample): Mono<Sample> {
    return entityTemplate.insert(sample)
      .flatMap { otherService.insertWithTransaction(sample.copy(id = sample.id + 1)) }
  }

  @Transactional(readOnly = true)
  fun insertWithReadOnlyTransaction(sample: Sample): Mono<Sample> {
    return entityTemplate.insert(sample)
  }

  @Transactional(readOnly = false)
  fun databaseClientWithTransactionRollback1(): Mono<Void> {
    val sql1 = "insert into sample(id, name) values (1, 't11')"
    val sql2 = "insert into sample(id, name) values (1, 't12')"
    return databaseClient.sql(sql1).fetch().rowsUpdated() // insert it
      .flatMap { databaseClient.sql(sql2).then() } // repeat insert it to raise error
  }

  @Transactional(readOnly = false)
  fun databaseClientWithTransactionRollback2(): Mono<Void> {
    val sample1 = Sample(id = 1, name = "t21")
    val sample2 = Sample(id = 1, name = "t22")
    return databaseClient.insert(table = "sample", entity = sample1, autoGenerateId = false) // insert it
      .flatMap {
        // repeat insert it to raise error
        databaseClient.insert(table = "sample", entity = sample2, autoGenerateId = false).then()
      }
  }

  companion object {
    val SAMPLE = Sample(1, "tester")
  }
}
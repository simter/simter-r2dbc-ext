package tech.simter.r2dbc

import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

@Service
class SampleService(private val entityTemplate: R2dbcEntityTemplate) {
  fun insertFailedByUniqueWithoutTransaction(): Mono<Sample> {
    return entityTemplate.insert(SAMPLE)
      .then(entityTemplate.insert(SAMPLE))
  }

  @Transactional(readOnly = false)
  fun insertFailedByUniqueWithTransaction(): Mono<Sample> {
    return entityTemplate.insert(SAMPLE)
      .then(entityTemplate.insert(SAMPLE))
  }

  @Transactional(readOnly = true)
  fun insertFailedByReadonlyTransaction(): Mono<Sample> {
    return entityTemplate.insert(SAMPLE)
  }

  companion object {
    val SAMPLE = Sample(1, "tester")
  }
}
package tech.simter.r2dbc.kotlin.repository

import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

@Service
class OtherService(private val entityTemplate: R2dbcEntityTemplate) {
  @Transactional(readOnly = false)
  fun insertWithTransaction(sample: Sample): Mono<Sample> {
    return entityTemplate.insert(sample)
  }
}
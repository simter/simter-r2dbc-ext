package tech.simter.r2dbc

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.core.publisher.Mono
import reactor.kotlin.test.test

/**
 * Test Transactional.
 *
 * @author RJ
 */
@DataR2dbcTest
@SpringJUnitConfig(UnitTestConfiguration::class)
class SampleServiceTransactionTest @Autowired constructor(
  private val entityTemplate: R2dbcEntityTemplate,
  private val service: SampleService
) {
  private fun getSample(): Mono<Sample> = entityTemplate
    .selectOne(Query.query(Criteria.where("id").`is`(SampleService.SAMPLE.id)), Sample::class.java)

  @BeforeEach
  fun clear() {
    entityTemplate.delete(SampleService.SAMPLE).block()
  }

  @Test
  fun failedByUniqueWithTransaction() {
    // insert failed by transaction rollback
    service.insertFailedByUniqueWithTransaction()
      .test()
      .expectError()
      .verify()

    // verify not insert
    getSample().test().verifyComplete()
  }

  /**
   * 2020-12-17 RJ: Failed on spring-boot-2.4.1
   * 2022-08-09 RJ: Failed on spring-boot-2.7.2
   */
  @Disabled
  @Test
  fun failedByReadonlyTransaction() {
    service.insertFailedByReadonlyTransaction()
      .test()
      .expectError()
      .verify()
  }

  @Test
  fun withoutTransaction() {
    // insert: success one and failed one
    service.insertFailedByUniqueWithoutTransaction()
      .test()
      .expectError()
      .verify()

    // verify the success one
    getSample().test()
      .expectNext(SampleService.SAMPLE)
      .verifyComplete()
  }
}
package tech.simter.r2dbc.kotlin.repository

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.core.publisher.Mono
import reactor.kotlin.test.test
import tech.simter.r2dbc.UnitTestConfiguration

/**
 * Test Transactional.
 *
 * @author RJ
 */
@DataR2dbcTest
@SpringJUnitConfig(UnitTestConfiguration::class)
class TransactionTest @Autowired constructor(
  private val entityTemplate: R2dbcEntityTemplate,
  private val sampleRepository: SampleRepository,
  private val sampleService: SampleService,
) {
  private fun getSample(): Mono<Sample> = entityTemplate
    .selectOne(Query.query(Criteria.where("id").`is`(SampleService.SAMPLE.id)), Sample::class.java)

  @BeforeEach
  fun clear() {
    entityTemplate.delete(SampleService.SAMPLE).subscribe()
  }

  @Test
  fun `R2dbcEntityTemplate - rollback by unique id with transaction`() {
    // insert failed by transaction rollback
    sampleService.insertFailedByUniqueWithTransaction()
      .test()
      .expectError(DataIntegrityViolationException::class.java)
      .verify()

    // verify not insert
    getSample().test().verifyComplete()
  }

  /**
   * 2020-12-17 RJ: Failed on spring-boot-2.4.1
   * 2022-08-09 RJ: Failed on spring-boot-2.7.2
   */
  //@Disabled
  @Test
  fun `insert with readonly transaction should failed`() {
    sampleService.insertWithReadOnlyTransaction(Sample(id = 1, name = "test"))
      .test()
      .expectError()// should raise readonly transaction error
      .verify()
  }

  @Test
  fun `R2dbcEntityTemplate - without transaction`() {
    // insert: success one and failed one
    sampleService.insertFailedByUniqueWithoutTransaction()
      .test()
      .expectError(DataIntegrityViolationException::class.java)
      .verify()

    // verify the success one
    getSample().test()
      .expectNext(SampleService.SAMPLE)
      .verifyComplete()
  }

  @Test
  fun `DatabaseClient 1 - rollback by unique id `() {
    // insert failed by transaction rollback
    sampleService.databaseClientWithTransactionRollback1()
      //.doOnError(System.out::println) // print out error
      .test()
      .expectError(DataIntegrityViolationException::class.java)
      .verify()

    // verify not insert
    sampleRepository.findAll().test().verifyComplete()
  }

  @Test
  fun `DatabaseClient 2 - rollback by unique id `() {
    // insert failed by transaction rollback
    sampleService.databaseClientWithTransactionRollback2()
      //.doOnError(System.out::println) // print out error
      .test()
      .expectError(DataIntegrityViolationException::class.java)
      .verify()

    // verify not insert
    sampleRepository.findAll().test().verifyComplete()
  }

  /**log indicate that the two service method run in the same transaction context
  DEBUG o.s.r2dbc.core.DefaultDatabaseClient : Executing SQL statement [DELETE FROM sample WHERE sample.id = $1]
  DEBUG o.s.r.c.R2dbcTransactionManager      : Creating new transaction with name [tech.simter.r2dbc.kotlin.repository.SampleService.insertWithOtherServiceTransaction]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
  DEBUG o.s.r.c.R2dbcTransactionManager      : Acquired Connection [MonoRetry] for R2DBC transaction
  DEBUG o.s.r.c.R2dbcTransactionManager      : Switching R2DBC Connection [PooledConnection[io.r2dbc.h2.H2Connection@3c9f4376]] to manual commit
  DEBUG o.s.r2dbc.core.DefaultDatabaseClient : Executing SQL statement [INSERT INTO sample (id, name) VALUES ($1, $2)]
  DEBUG o.s.r.c.R2dbcTransactionManager      : Participating in existing transaction
  DEBUG o.s.r2dbc.core.DefaultDatabaseClient : Executing SQL statement [INSERT INTO sample (id, name) VALUES ($1, $2)]
  DEBUG o.s.r.c.R2dbcTransactionManager      : Initiating transaction commit
  DEBUG o.s.r.c.R2dbcTransactionManager      : Committing R2DBC transaction on Connection [PooledConnection[io.r2dbc.h2.H2Connection@3c9f4376]]
  DEBUG o.s.r.c.R2dbcTransactionManager      : Releasing R2DBC Connection [PooledConnection[io.r2dbc.h2.H2Connection@3c9f4376]] after transaction
  DEBUG o.s.r2dbc.core.DefaultDatabaseClient : Executing SQL statement [SELECT sample.* FROM sample]
   */
  @Test
  fun `call multiple service with transaction`() {
    val sample1 = Sample(id = 1, name = "t1")
    val sample2 = sample1.copy(id = sample1.id + 1)
    sampleService.insertWithOtherServiceTransaction(sample1)
      .test()
      .expectNext(sample2)
      .verifyComplete()

    // verify not insert
    sampleRepository.findAll().test().expectNextCount(2).verifyComplete()
  }
}
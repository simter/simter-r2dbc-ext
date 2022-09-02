package tech.simter.r2dbc.kotlin.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.kotlin.test.test
import tech.simter.r2dbc.UnitTestConfiguration

/**
 * Test Transactional.
 *
 * @author RJ
 */
@DataR2dbcTest
@SpringJUnitConfig(UnitTestConfiguration::class)
class AutoIdTest @Autowired constructor(
  private val entityTemplate: R2dbcEntityTemplate,
  private val repository: SampleRepository
) {
  @BeforeEach
  fun clear() {
    repository.deleteAll().subscribe()
  }

  @Test
  fun `do insert if id eq 0`() {
    // do insert
    var id = 0
    val sample = Sample(id = 0, name = "tester")
    repository.save(sample)
      .test()
      .assertNext {
        id = it.id
        assertThat(it.id).isGreaterThan(0)
      }
      .verifyComplete()

    // verify
    repository.findById(id)
      .test()
      .expectNext(sample.copy(id = id))
      .verifyComplete()
  }

  @Test
  fun `do update if id gt 0`() {
    // prepare data
    val source = Sample(id = 1, name = "t1")
    entityTemplate.insert(source)
      .test()
      .expectNext(source)
      .verifyComplete()

    // do update
    val target = source.copy(name = "t2")
    repository.save(target)
      .test()
      .expectNext(target)
      .verifyComplete()

    // verify
    repository.findById(target.id)
      .test()
      .expectNext(target)
      .verifyComplete()
  }
}
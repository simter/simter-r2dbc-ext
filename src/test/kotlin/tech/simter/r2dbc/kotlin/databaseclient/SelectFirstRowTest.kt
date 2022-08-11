package tech.simter.r2dbc.kotlin.databaseclient

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.kotlin.test.test
import tech.simter.r2dbc.UnitTestConfiguration
import tech.simter.r2dbc.kotlin.databaseclient.Helper.clean
import tech.simter.r2dbc.kotlin.databaseclient.Helper.initTable
import tech.simter.r2dbc.kotlin.databaseclient.Helper.insertOne
import tech.simter.r2dbc.kotlin.selectFirstRow
import tech.simter.util.RandomUtils.randomString
import java.time.LocalDate

@DataR2dbcTest
@SpringJUnitConfig(UnitTestConfiguration::class)
@TestInstance(PER_CLASS)
class SelectFirstRowTest @Autowired constructor(private val databaseClient: DatabaseClient) {
  @BeforeAll
  fun init() {
    initTable(databaseClient).subscribe()
  }

  @BeforeEach
  fun clean() {
    clean(databaseClient).subscribe()
  }

  @Test
  fun `return empty`() {
    databaseClient.selectFirstRow<Sample2>("select id, ts, the_name from sample2")
      .test().verifyComplete()
  }

  @Test
  fun `return one row`() {
    // insert row1
    val source1 = Sample2(id = 1, ts = LocalDate.now(), theName = randomString(6))
    insertOne(databaseClient, source1)
      .test()
      .expectNext(source1)
      .verifyComplete()

    // select row1
    databaseClient.selectFirstRow<Sample2>("select id, ts, the_name from sample2")
      .test()
      .expectNext(source1)
      .verifyComplete()

    // insert row2
    val source2 = source1.copy(id = source1.id + 1)
    insertOne(databaseClient, source2)
      .test()
      .expectNext(source2)
      .verifyComplete()

    // select row2
    databaseClient.selectFirstRow<Sample2>("select id, ts, the_name from sample2 order by id desc")
      .test()
      .expectNext(source2)
      .verifyComplete()
  }
}
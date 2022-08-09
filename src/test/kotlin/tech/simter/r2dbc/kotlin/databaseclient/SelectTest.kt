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
import tech.simter.r2dbc.kotlin.select
import tech.simter.util.RandomUtils.randomString
import java.time.LocalDate

@DataR2dbcTest
@SpringJUnitConfig(UnitTestConfiguration::class)
@TestInstance(PER_CLASS)
class SelectTest @Autowired constructor(private val databaseClient: DatabaseClient) {
  @BeforeAll
  fun init() {
    initTable(databaseClient).subscribe()
  }

  @BeforeEach
  fun clean() {
    clean(databaseClient).subscribe()
  }

  @Test
  fun `bind value`() {
    // init data
    val source = Sample(id = 1, ts = LocalDate.now(), theName = randomString(6))
    insertOne(databaseClient, source)
      .test()
      .expectNext(source)
      .verifyComplete()

    // select
    databaseClient.select<Sample>(
      sql = "select id, ts, the_name from sample where id = :id",
      params = mapOf("id" to source.id)
    ).test()
      .expectNext(source)
      .verifyComplete()
  }

  @Test
  fun `with name exclude`() {
    // init data
    val source = Sample(id = 1, ts = LocalDate.now(), theName = randomString(6))
    insertOne(databaseClient, source)
      .test()
      .expectNext(source)
      .verifyComplete()

    // select
    databaseClient.select<Sample>(
      sql = "select id, ts, the_name, creator from sample where id = :id",
      params = mapOf("id" to source.id),
      // these make theName and createBy has the default value
      excludeNames = listOf("theName", "createBy")
    ).test()
      .expectNext(Sample(id = source.id, ts = source.ts))
      .verifyComplete()
  }

  @Test
  fun `with name mapper`() {
    // init data
    val source = Sample(id = 1, ts = LocalDate.now(), theName = randomString(6))
    insertOne(databaseClient, source)
      .test()
      .expectNext(source)
      .verifyComplete()

    // select
    databaseClient.select<Sample>(
      sql = "select id, ts, the_name, creator from sample where id = :id",
      params = mapOf("id" to source.id),
      nameMapper = mapOf("createBy" to "creator")
    ).test()
      .expectNext(source)
      .verifyComplete()
  }

  @Test
  fun `with value mapper`() {
    // init data
    val source = Sample(id = 1, ts = LocalDate.now(), theName = randomString(6))
    insertOne(databaseClient, source)
      .test()
      .expectNext(source)
      .verifyComplete()

    // select (the creator column value would be ignored because no matcher nameMapper)
    databaseClient.select<Sample>(
      sql = "select id, ts, the_name, creator from sample where id = :id",
      params = mapOf("id" to source.id),
      valueMapper = mapOf("theName" to { value -> "$value-more" })
    ).test()
      .expectNext(source.copy(theName = "${source.theName}-more"))
      .verifyComplete()
  }
}
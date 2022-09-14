package tech.simter.r2dbc.kotlin.databaseclient

import org.assertj.core.api.Assertions.assertThat
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
import tech.simter.r2dbc.kotlin.selectToMap
import tech.simter.util.RandomUtils.randomString
import java.time.LocalDate

@DataR2dbcTest
@SpringJUnitConfig(UnitTestConfiguration::class)
@TestInstance(PER_CLASS)
class SelectToMapTest @Autowired constructor(private val databaseClient: DatabaseClient) {
  private val valueTypes = mapOf(
    "id" to Int::class.javaObjectType,
    "ts" to LocalDate::class.java,
    "theName" to String::class.java,
    "creator" to String::class.java,
  )

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
    val ts = LocalDate.now()
    val theName = randomString(6)
    val source = Sample2(id = 1, ts = ts, theName = theName, createBy = null)
    insertOne(databaseClient, source)
      .test()
      .expectNext(source)
      .verifyComplete()

    // select
    databaseClient.selectToMap(
      sql = "select id, ts, the_name, creator from sample2 where id = :id",
      params = mapOf("id" to source.id),
      valueTypes = valueTypes,
    ).test().expectNext(
      mapOf(
        "id" to 1,
        "ts" to ts,
        "theName" to theName,
        "creator" to null,
      )
    ).verifyComplete()
  }

  @Test
  fun `with value mapper`() {
    // init data
    val ts = LocalDate.now()
    val theName = randomString(6)
    val source = Sample2(id = 1, ts = ts, theName = theName, createBy = null)
    insertOne(databaseClient, source)
      .test()
      .expectNext(source)
      .verifyComplete()

    // select
    databaseClient.selectToMap(
      sql = "select id, ts, the_name, creator from sample2 where id = :id",
      params = mapOf("id" to source.id),
      valueTypes = valueTypes,
      valueMapper = mapOf("theName" to { value -> "$value-more" }),
    ).test().expectNext(
      mapOf(
        "id" to 1,
        "ts" to ts,
        "theName" to "${theName}-more",
        "creator" to null,
      )
    ).verifyComplete()
  }

  @Test
  fun `with custom name mapper`() {
    // init data
    val ts = LocalDate.now()
    val theName = randomString(6)
    val source = Sample2(id = 1, ts = ts, theName = theName, createBy = null)
    insertOne(databaseClient, source)
      .test()
      .expectNext(source)
      .verifyComplete()

    // select
    databaseClient.selectToMap(
      sql = "select id, ts, the_name, creator from sample2 where id = :id",
      params = mapOf("id" to source.id),
      valueTypes = mapOf(
        "id" to Int::class.javaObjectType,
        "ts" to LocalDate::class.java,
        "the_name" to String::class.java,
        "creator" to String::class.java,
      ),
      nameMapper = { name -> name.lowercase() },
    ).test().expectNext(
      mapOf(
        "id" to 1,
        "ts" to ts,
        "the_name" to theName,
        "creator" to null,
      )
    ).verifyComplete()
  }

  @Test
  fun `missing value type`() {
    // init data
    val ts = LocalDate.now()
    val theName = randomString(6)
    val source = Sample2(id = 1, ts = ts, theName = theName, createBy = null)
    insertOne(databaseClient, source)
      .test()
      .expectNext(source)
      .verifyComplete()

    // select
    databaseClient.selectToMap(
      sql = "select id, ts, the_name, creator from sample2 where id = :id",
      params = mapOf("id" to source.id),
      valueTypes = mapOf(
        "id" to Int::class.javaObjectType,
        "ts" to LocalDate::class.java,
        "creator" to String::class.java,
      ),
    ).test().consumeErrorWith {
      assertThat(it).isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Missing value-type config for name 'theName'")
    }.verify()
  }
}
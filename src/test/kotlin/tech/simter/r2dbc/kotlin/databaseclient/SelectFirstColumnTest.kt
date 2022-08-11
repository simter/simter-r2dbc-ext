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
import tech.simter.r2dbc.kotlin.databaseclient.Sample2.Status
import tech.simter.r2dbc.kotlin.selectFirstColumn
import tech.simter.util.RandomUtils.randomString
import java.time.LocalDate
import java.util.*

@DataR2dbcTest
@SpringJUnitConfig(UnitTestConfiguration::class)
@TestInstance(PER_CLASS)
class SelectFirstColumnTest @Autowired constructor(private val databaseClient: DatabaseClient) {
  @BeforeAll
  fun init() {
    initTable(databaseClient).subscribe()
  }

  @BeforeEach
  fun clean() {
    clean(databaseClient).subscribe()
  }

  @Test
  fun simple() {
    // init data
    val source = Sample2(id = 1, ts = LocalDate.now(), theName = randomString(6))
    insertOne(databaseClient, source)
      .test()
      .expectNext(source)
      .verifyComplete()

    // select Int
    databaseClient.selectFirstColumn<Int>("select id from sample2")
      .test()
      .expectNext(source.id)
      .verifyComplete()

    // select String
    databaseClient.selectFirstColumn<String>("select the_name from sample2")
      .test()
      .expectNext(source.theName!!)
      .verifyComplete()

    // select LocalDate
    databaseClient.selectFirstColumn<LocalDate>("select ts from sample2")
      .test()
      .expectNext(source.ts)
      .verifyComplete()
  }

  @Test
  fun `with bind value`() {
    // init data
    val source = Sample2(id = 1, ts = LocalDate.now(), theName = randomString(6))
    insertOne(databaseClient, source)
      .test()
      .expectNext(source)
      .verifyComplete()

    // select Int
    databaseClient.selectFirstColumn<Int>(
      sql = "select id from sample2 where id = :id",
      params = mapOf("id" to source.id),
    ).test()
      .expectNext(source.id)
      .verifyComplete()

    // select String
    databaseClient.selectFirstColumn<String>(
      sql = "select the_name from sample2 where id = :id",
      params = mapOf("id" to source.id),
    ).test()
      .expectNext(source.theName!!)
      .verifyComplete()

    // select LocalDate
    databaseClient.selectFirstColumn<LocalDate>(
      sql = "select ts from sample2 where id = :id",
      params = mapOf("id" to source.id),
    ).test()
      .expectNext(source.ts)
      .verifyComplete()
  }

  @Test
  fun `with value mapper`() {
    // init data
    val source = Sample2(id = 1, ts = LocalDate.now(), theName = randomString(6))
    insertOne(databaseClient, source)
      .test()
      .expectNext(source)
      .verifyComplete()

    // select Int
    databaseClient.selectFirstColumn<Int>(
      sql = "select id from sample2",
      valueMapper = { it as Int + 1 },
    ).test()
      .expectNext(source.id + 1)
      .verifyComplete()

    // select String
    databaseClient.selectFirstColumn<String>(
      sql = "select the_name from sample2",
      valueMapper = { "$it-more" },
    ).test()
      .expectNext("${source.theName}-more")
      .verifyComplete()

    // select LocalDate
    databaseClient.selectFirstColumn<LocalDate>(
      sql = "select ts from sample2",
      valueMapper = { (it as LocalDate).minusDays(1) },
    ).test()
      .expectNext(source.ts.minusDays(1))
      .verifyComplete()

    // select Status
    databaseClient.selectFirstColumn<Status>(
      sql = "select status from sample2",
      valueMapper = { value -> Status.values().first { it.name == value } },
    ).test()
      .expectNext(source.status)
      .verifyComplete()
  }

  @Test
  fun `error with null value`() {
    // init data
    val source = Sample2(id = 1, ts = LocalDate.now(), theName = null)
    insertOne(databaseClient, source)
      .test()
      .expectNext(source)
      .verifyComplete()

    // select String
    databaseClient.selectFirstColumn<String>("select the_name from sample2")
      .test()
      .expectError(NullPointerException::class.java)
      .verify()
  }

  @Test
  fun `mapping null value to empty optional`() {
    // init data
    val source = Sample2(id = 1, ts = LocalDate.now(), theName = null)
    insertOne(databaseClient, source)
      .test()
      .expectNext(source)
      .verifyComplete()

    // select String
    databaseClient.selectFirstColumn<Optional<String>>(
      sql = "select the_name from sample2",
      valueMapper = { Optional.ofNullable(it as String?) },
    )
      .test()
      .expectNext(Optional.empty())
      .verifyComplete()
  }
}
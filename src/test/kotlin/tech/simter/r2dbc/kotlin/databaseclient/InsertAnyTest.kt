package tech.simter.r2dbc.kotlin.databaseclient

import io.r2dbc.spi.Row
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
import tech.simter.r2dbc.kotlin.databaseclient.Sample2.Companion.STATUS_2_DB_VALUE
import tech.simter.r2dbc.kotlin.insertAny
import tech.simter.util.RandomUtils.randomString
import java.time.LocalDate

@DataR2dbcTest
@SpringJUnitConfig(UnitTestConfiguration::class)
@TestInstance(PER_CLASS)
class InsertAnyTest @Autowired constructor(private val databaseClient: DatabaseClient) {
  @BeforeAll
  fun init() {
    initTable(databaseClient).subscribe()
  }

  @BeforeEach
  fun clean() {
    clean(databaseClient).subscribe()
  }

  @Test
  fun `default exclude null value`() {
    // insert
    val theName = randomString(6)
    databaseClient.insertAny(
      table = "sample2",
      entity = Sample2(ts = LocalDate.now(), theName = theName),
      excludeNames = listOf("id", "status"),
    ).test().verifyComplete()

    // verify
    databaseClient.sql("select the_name from sample2 where the_name = :theName")
      .bind("theName", theName)
      .map { row: Row -> row.get("the_name") as String }
      .one()
      .test()
      .expectNext(theName)
      .verifyComplete()
  }

  @Test
  fun `custom id`() {
    // insert
    val id = 100
    val theName = randomString(6)
    databaseClient.insertAny(
      table = "sample2",
      entity = Sample2(id = id, ts = LocalDate.now(), theName = theName),
      excludeNames = listOf("status"),
    ).test().verifyComplete()

    // verify
    databaseClient.sql("select the_name from sample2 where id = :id")
      .bind("id", id)
      .map { row: Row -> row.get("the_name") as String }
      .one()
      .test()
      .expectNext(theName)
      .verifyComplete()
  }

  @Test
  fun `with exclude names`() {
    // insert
    val id = 100
    val theName = randomString(6)
    databaseClient.insertAny(
      table = "sample2",
      entity = Sample2(id = id, ts = LocalDate.now(), theName = theName),
      excludeNames = listOf("name", "status"),
    ).test().verifyComplete()

    // verify
    databaseClient.sql("select the_name from sample2 where id = :id")
      .bind("id", id)
      .map { row: Row -> row.get("the_name") as String }
      .one()
      .test()
      .expectNext(theName)
      .verifyComplete()
  }

  @Test
  fun `with name mapper`() {
    // insert
    val id = 100
    val createBy = randomString(6)
    databaseClient.insertAny(
      table = "sample2",
      entity = Sample2(id = id, ts = LocalDate.now(), createBy = createBy),
      nameMapper = mapOf("createBy" to "creator"),
      excludeNames = listOf("status"),
    ).test().verifyComplete()

    // verify
    databaseClient.sql("select creator from sample2 where id = :id")
      .bind("id", id)
      .map { row: Row -> row.get("creator") as String }
      .one()
      .test()
      .expectNext(createBy)
      .verifyComplete()
  }

  @Test
  fun `with name mapper and include null value`() {
    // insert
    val id = 100
    val createBy = randomString(6)
    databaseClient.insertAny(
      table = "sample2",
      entity = Sample2(id = id, ts = LocalDate.now(), createBy = createBy),
      includeNullValue = true,
      nameMapper = mapOf("createBy" to "creator"),
      excludeNames = listOf("status"),
    ).test().verifyComplete()

    // verify
    databaseClient.sql("select creator from sample2 where id = :id")
      .bind("id", id)
      .map { row: Row -> row.get("creator") as String }
      .one()
      .test()
      .expectNext(createBy)
      .verifyComplete()
  }

  @Test
  fun `with value mapper`() {
    // insert
    val id = 100
    val theName = randomString(6)
    databaseClient.insertAny(
      table = "sample2",
      entity = Sample2(id = id, ts = LocalDate.now(), theName = theName),
      valueMapper = mapOf(
        "theName" to { value -> "$value-more" },
        STATUS_2_DB_VALUE,
      ),
    ).test().verifyComplete()

    // verify
    databaseClient.sql("select the_name from sample2 where id = :id")
      .bind("id", id)
      .map { row: Row -> row.get("the_name") as String }
      .one()
      .test()
      .expectNext("$theName-more")
      .verifyComplete()
  }

  @Test
  fun `with external column`() {
    // insert
    val id = 10
    val pid = 100
    databaseClient.insertAny(
      table = "sample2",
      entity = Sample2(id = id, ts = LocalDate.now()),
      excludeNames = listOf("status"),
      externalColumnValues = mapOf("pid" to pid),
    ).test().verifyComplete()

    // verify
    databaseClient.sql("select pid from sample2 where id = :id")
      .bind("id", id)
      .map { row: Row -> row.get("pid") as Int }
      .one()
      .test()
      .expectNext(pid)
      .verifyComplete()
  }
}
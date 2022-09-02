package tech.simter.r2dbc.kotlin.databaseclient

import io.r2dbc.spi.Row
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
import tech.simter.r2dbc.kotlin.batchInsert
import tech.simter.r2dbc.kotlin.databaseclient.Helper.clean
import tech.simter.r2dbc.kotlin.databaseclient.Helper.initTable
import tech.simter.r2dbc.kotlin.databaseclient.Sample2.Companion.STATUS_2_DB_VALUE
import tech.simter.util.RandomUtils.randomString
import java.time.LocalDate

@DataR2dbcTest
@SpringJUnitConfig(UnitTestConfiguration::class)
@TestInstance(PER_CLASS)
class BatchInsertTest @Autowired constructor(private val databaseClient: DatabaseClient) {
  @BeforeAll
  fun init() {
    initTable(databaseClient).subscribe()
  }

  @BeforeEach
  fun clean() {
    clean(databaseClient).subscribe()
  }

  @Test
  fun `insert nothing`() {
    databaseClient.batchInsert(
      table = "sample2",
      entities = emptyList<Sample2>()
    ).test().expectNext(emptyList()).verifyComplete()
  }

  @Test
  fun `insert one`() {
    // insert
    val s = Sample2(id = 100, ts = LocalDate.now(), theName = randomString(6))
    databaseClient.batchInsert(
      table = "sample2",
      entities = listOf(s),
      autoGenerateId = false,
      excludeNames = listOf("status", "createBy"),
    ).test()
      .expectNext(listOf(s.id))
      .verifyComplete()

    // verify
    databaseClient.sql("select the_name from sample2 where id = :id")
      .bind("id", s.id)
      .map { row: Row -> row.get("the_name") as String }
      .all()
      .test()
      .expectNext(s.theName!!)
      .verifyComplete()
  }

  @Test
  fun `insert with custom id`() {
    // insert
    val s1 = Sample2(id = 100, ts = LocalDate.now(), theName = randomString(6))
    val s2 = Sample2(id = 101, ts = LocalDate.now(), theName = randomString(6))
    databaseClient.batchInsert(
      table = "sample2",
      entities = listOf(s1, s2),
      autoGenerateId = false,
      excludeNames = listOf("status", "createBy"),
    ).test()
      .assertNext { assertThat(it).isEqualTo(listOf(s1.id, s2.id)) }
      .verifyComplete()

    // verify
    databaseClient.sql("select the_name from sample2 where id in (:ids) order by id asc")
      .bind("ids", listOf(s1.id, s2.id))
      .map { row: Row -> row.get("the_name") as String }
      .all()
      .test()
      .expectNext(s1.theName!!)
      .expectNext(s2.theName!!)
      .verifyComplete()
  }

  @Test
  fun `insert with auto id`() {
    // insert
    val s1 = Sample2(id = 0, ts = LocalDate.now(), theName = randomString(6))
    val s2 = Sample2(id = 0, ts = LocalDate.now(), theName = randomString(6))
    val autoIds = mutableListOf<Int>()
    databaseClient.batchInsert(
      table = "sample2",
      entities = listOf(s1, s2),
      autoGenerateId = true,
      excludeNames = listOf("status", "createBy"),
    ).test()
      .assertNext {
        assertThat(it[0]).isGreaterThan(0)
        assertThat(it[1]).isGreaterThan(it[0])
        autoIds.add(it[0])
        autoIds.add(it[1])
      }
      .verifyComplete()

    // verify
    databaseClient.sql("select the_name from sample2 where id in (:ids) order by id asc")
      .bind("ids", autoIds)
      .map { row: Row -> row.get("the_name") as String }
      .all()
      .test()
      .expectNext(s1.theName!!)
      .expectNext(s2.theName!!)
      .verifyComplete()
  }

  @Test
  fun `with name mapper`() {
    // insert
    val s = Sample2(id = 100, ts = LocalDate.now(), createBy = randomString(6))
    databaseClient.batchInsert(
      table = "sample2",
      entities = listOf(s),
      autoGenerateId = false,
      nameMapper = mapOf("createBy" to "creator"),
      excludeNames = listOf("status"),
    ).test()
      .expectNext(listOf(s.id))
      .verifyComplete()

    // verify
    databaseClient.sql("select creator from sample2 where id = :id")
      .bind("id", s.id)
      .map { row: Row -> row.get("creator") as String }
      .one()
      .test()
      .expectNext(s.createBy!!)
      .verifyComplete()
  }

  @Test
  fun `with value mapper`() {
    // insert
    var id = 0
    val s = Sample2(id = id, ts = LocalDate.now(), theName = randomString(6))
    databaseClient.batchInsert(
      table = "sample2",
      entities = listOf(s),
      valueMapper = mapOf(
        "theName" to { value -> "$value-more" },
        STATUS_2_DB_VALUE,
      ),
      excludeNames = listOf("createBy"),
    ).test()
      .assertNext {
        id = it[0]
        assertThat(id).isGreaterThan(0)
      }
      .verifyComplete()

    // verify
    databaseClient.sql("select the_name from sample2 where id = :id")
      .bind("id", id)
      .map { row: Row -> row.get("the_name") as String }
      .one()
      .test()
      .expectNext("${s.theName}-more")
      .verifyComplete()
  }

  @Test
  fun `insert with external column`() {
    // insert
    val pid = 100
    val s = Sample2(id = 100, ts = LocalDate.now())
    databaseClient.batchInsert(
      table = "sample2",
      entities = listOf(s),
      autoGenerateId = false,
      excludeNames = listOf("status", "createBy"),
      externalColumnValues = mapOf("pid" to pid),
    ).test()
      .expectNext(listOf(s.id))
      .verifyComplete()

    // verify
    databaseClient.sql("select pid from sample2 where id = :id")
      .bind("id", s.id)
      .map { row: Row -> row.get("pid") as Int }
      .one()
      .test()
      .expectNext(pid)
      .verifyComplete()
  }
}
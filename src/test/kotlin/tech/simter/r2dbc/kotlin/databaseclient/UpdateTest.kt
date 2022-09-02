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
import tech.simter.r2dbc.kotlin.databaseclient.Sample2.Companion.DB_2_STATUS_VALUE
import tech.simter.r2dbc.kotlin.databaseclient.Sample2.Companion.STATUS_2_DB_VALUE
import tech.simter.r2dbc.kotlin.databaseclient.Sample2.Status.First
import tech.simter.r2dbc.kotlin.databaseclient.Sample2.Status.Second
import tech.simter.r2dbc.kotlin.select
import tech.simter.r2dbc.kotlin.selectFirstColumn
import tech.simter.r2dbc.kotlin.update
import tech.simter.util.RandomUtils.randomString
import java.time.LocalDate

@DataR2dbcTest
@SpringJUnitConfig(UnitTestConfiguration::class)
@TestInstance(PER_CLASS)
class UpdateTest @Autowired constructor(private val databaseClient: DatabaseClient) {
  @BeforeAll
  fun init() {
    initTable(databaseClient).subscribe()
  }

  @BeforeEach
  fun clean() {
    clean(databaseClient).subscribe()
  }

  @Test
  fun `update default`() {
    // init data
    val source =
      Sample2(id = 1, ts = LocalDate.now(), status = First, theName = randomString(6), createBy = randomString(6))
    insertOne(databaseClient, source)
      .test()
      .expectNext(source)
      .verifyComplete()

    // do update
    databaseClient.update(
      table = "sample2",
      sets = mapOf<String, Any?>(
        "theName" to "${source.theName}-newValue",
      ),
    ).test()
      .expectNext(1)
      .verifyComplete()

    // verify updated
    databaseClient.selectFirstColumn<String>(
      sql = "select the_name from sample2",
    ).test()
      .expectNext("${source.theName}-newValue")
      .verifyComplete()
  }

  @Test
  fun `with name and value mapper`() {
    // init data
    val source =
      Sample2(id = 1, ts = LocalDate.now(), status = First, theName = randomString(6), createBy = randomString(6))
    insertOne(databaseClient, source)
      .test()
      .expectNext(source)
      .verifyComplete()

    // do update
    val target =
      source.copy(ts = source.ts.minusDays(1), status = Second, theName = randomString(6), createBy = randomString(6))
    databaseClient.update(
      table = "sample2",
      sets = mapOf<String, Any?>(
        "ts" to target.ts,
        "status" to target.status,
        "theName" to target.theName,
        "createBy" to target.createBy,
      ),
      whereSql = "where id = :id",
      whereParams = mapOf<String, Any>("id" to target.id),
      nameMapper = mapOf("createBy" to "creator"),
      valueMapper = mapOf(STATUS_2_DB_VALUE),
    ).test()
      .expectNext(1)
      .verifyComplete()

    // verify updated
    databaseClient.select<Sample2>(
      sql = "select * from sample2 where id = :id",
      params = mapOf("id" to target.id),
      nameMapper = mapOf("createBy" to "creator"),
      valueMapper = mapOf(DB_2_STATUS_VALUE),
    ).test()
      .expectNext(target)
      .verifyComplete()
  }
}
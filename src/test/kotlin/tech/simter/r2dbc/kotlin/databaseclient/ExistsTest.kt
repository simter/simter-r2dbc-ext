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
import tech.simter.r2dbc.kotlin.exists
import tech.simter.util.RandomUtils.randomString
import java.time.LocalDate

@DataR2dbcTest
@SpringJUnitConfig(UnitTestConfiguration::class)
@TestInstance(PER_CLASS)
class ExistsTest @Autowired constructor(private val databaseClient: DatabaseClient) {
  @BeforeAll
  fun init() {
    initTable(databaseClient).subscribe()
  }

  @BeforeEach
  fun clean() {
    clean(databaseClient).subscribe()
  }

  @Test
  fun `not exists`() {
    databaseClient.exists("select 0 from sample2").test().expectNext(false).verifyComplete()
  }

  @Test
  fun `exists without param`() {
    // init data
    val source = Sample2(id = 1, ts = LocalDate.now(), theName = randomString(6))
    insertOne(databaseClient, source).test().expectNext(source).verifyComplete()

    // do it
    databaseClient.exists("select 0 from sample2").test().expectNext(true).verifyComplete()
  }

  @Test
  fun `exists with param`() {
    // init data
    val source = Sample2(id = 1, ts = LocalDate.now(), theName = randomString(6))
    insertOne(databaseClient, source).test().expectNext(source).verifyComplete()

    // do it
    databaseClient.exists(
      sql = "select 0 from sample2 where id = :id",
      params = mapOf("id" to source.id),
    ).test().expectNext(true).verifyComplete()
  }
}
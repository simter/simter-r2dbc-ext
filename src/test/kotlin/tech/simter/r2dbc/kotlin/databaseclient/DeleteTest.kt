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
import tech.simter.r2dbc.kotlin.databaseclient.Sample2.Status.First
import tech.simter.r2dbc.kotlin.delete
import tech.simter.util.RandomUtils.randomString
import java.time.LocalDate

@DataR2dbcTest
@SpringJUnitConfig(UnitTestConfiguration::class)
@TestInstance(PER_CLASS)
class DeleteTest @Autowired constructor(private val databaseClient: DatabaseClient) {
  @BeforeAll
  fun init() {
    initTable(databaseClient).subscribe()
  }

  @BeforeEach
  fun clean() {
    clean(databaseClient).subscribe()
  }

  @Test
  fun `delete nothing`() {
    databaseClient.delete("delete from sample2").test().expectNext(0).verifyComplete()
  }

  @Test
  fun `delete it`() {
    // init data
    val source =
      Sample2(id = 1, ts = LocalDate.now(), status = First, theName = randomString(6), createBy = randomString(6))
    insertOne(databaseClient, source)
      .test()
      .expectNext(source)
      .verifyComplete()

    // do delete
    databaseClient.delete(sql = "delete from sample2 where id = :id", params = mapOf("id" to 1))
      .test().expectNext(1).verifyComplete()
  }
}
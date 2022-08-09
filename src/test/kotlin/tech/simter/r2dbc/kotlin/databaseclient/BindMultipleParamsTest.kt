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
import tech.simter.r2dbc.kotlin.bind
import tech.simter.r2dbc.kotlin.bindNull
import tech.simter.r2dbc.kotlin.databaseclient.Helper.clean
import tech.simter.r2dbc.kotlin.databaseclient.Helper.initTable
import java.time.LocalDate

@DataR2dbcTest
@SpringJUnitConfig(UnitTestConfiguration::class)
@TestInstance(PER_CLASS)
class BindMultipleParamsTest @Autowired constructor(private val databaseClient: DatabaseClient) {
  @BeforeAll
  fun init() {
    initTable(databaseClient).subscribe()
  }

  @BeforeEach
  fun clean() {
    clean(databaseClient).subscribe()
  }

  @Test
  fun `bind not null params`() {
    databaseClient
      .sql("insert into sample(ts, the_name) values (:ts, :theName)")
      .bind(
        mapOf<String, Any>(
          "theName" to "test",
          "ts" to LocalDate.now()
        )
      )
      .fetch()
      .rowsUpdated()
      .test()
      .expectNext(1)
      .verifyComplete()
  }

  @Test
  fun `bind null params`() {
    databaseClient
      .sql("insert into sample(ts, the_name) values (:ts, :theName)")
      .bind("ts", LocalDate.now())
      .bindNull(
        mapOf<String, Class<*>>(
          "theName" to String::class.java
        )
      )
      .fetch()
      .rowsUpdated()
      .test()
      .expectNext(1)
      .verifyComplete()
  }
}
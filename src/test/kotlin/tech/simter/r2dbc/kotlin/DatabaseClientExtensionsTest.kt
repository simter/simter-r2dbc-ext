package tech.simter.r2dbc.kotlin

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.kotlin.test.test
import tech.simter.r2dbc.UnitTestConfiguration

@DataR2dbcTest
@SpringJUnitConfig(UnitTestConfiguration::class)
@TestInstance(PER_CLASS)
class DatabaseClientExtensionsTest @Autowired constructor(private val databaseClient: DatabaseClient) {
  @BeforeAll
  fun setup() {
    databaseClient.sql("""
        create table t(
          id varchar(10) not null primary key,
          name varchar(10),
          remark varchar(10)
        );""".trimIndent())
      .fetch()
      .rowsUpdated()
      .block()
  }

  @Test
  fun test() {
    val remark: String? = null // or = "test"
    databaseClient
      .sql("insert into t(id, name, remark) values (:id, :name, :remark)")
      .bind("id", "id")
      .bindNull("name", String::class.java)
      .bindNullable<String>("remark", remark)
      .fetch()
      .rowsUpdated()
      .test()
      .expectNext(1)
      .verifyComplete()
  }
}
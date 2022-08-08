package tech.simter.r2dbc.kotlin.databaseclient

import io.r2dbc.spi.Row
import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.publisher.Mono
import tech.simter.util.RandomUtils.randomString
import java.time.LocalDate

object Helper {
  fun initTable(databaseClient: DatabaseClient): Mono<Void> {
    return databaseClient.sql("drop table if exists sample")
      .then()
      .then(
        databaseClient.sql(
          """
        create table sample(
          id serial primary key,
          ts date not null,
          the_name varchar(255),
          creator varchar(255)
        )""".trimIndent()
        ).then()
      )
  }

  fun clean(databaseClient: DatabaseClient): Mono<Void> {
    return databaseClient.sql("delete from sample").then()
  }

  fun insertOne(databaseClient: DatabaseClient): Mono<Int> {
    return databaseClient
      .sql("insert into sample(ts, name) values (:ts, :name)")
      .bind("ts", LocalDate.now())
      .bind("name", randomString(4))
      .filter { s -> s.returnGeneratedValues("id") }
      .map { row: Row -> row.get("id") as Int } // get auto generated id
      .one()
  }
}
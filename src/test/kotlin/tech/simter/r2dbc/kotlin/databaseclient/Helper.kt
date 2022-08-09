package tech.simter.r2dbc.kotlin.databaseclient

import io.r2dbc.spi.Row
import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.publisher.Mono

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

  fun insertOne(databaseClient: DatabaseClient, sample: Sample): Mono<Sample> {
    val hasId = sample.id > 0
    var spec = databaseClient
      .sql("insert into sample(${if (hasId) "id, " else ""}ts, the_name, creator) values (${if (hasId) ":id, " else ""}:ts, :theName, :creator)")
      .bind("ts", sample.ts)

    // id
    if (hasId) spec = spec.bind("id", sample.id)

    // theName
    spec = if (sample.theName == null) spec.bindNull("theName", String::class.java)
    else spec.bind("theName", sample.theName)

    // creator
    spec = if (sample.createBy == null) spec.bindNull("creator", String::class.java)
    else spec.bind("creator", sample.createBy)

    return if (hasId) spec.then().thenReturn(sample)
    else spec
      .filter { s -> s.returnGeneratedValues("id") }
      .map { row: Row -> sample.copy(id = row.get("id") as Int) } // get auto generated id
      .one()
  }
}
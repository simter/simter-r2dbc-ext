package tech.simter.r2dbc.kotlin

import io.r2dbc.spi.Row
import org.springframework.data.relational.core.query.Query
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec
import reactor.core.publisher.Mono
import tech.simter.kotlin.data.Id
import tech.simter.util.StringUtils.underscore
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties

/**
 * Extension for mix [GenericExecuteSpec.bind] and [GenericExecuteSpec.bindNull]
 * providing a convenient method to auto bind nullable value.
 *
 * Usage:
 *
 * ```
 * val nullableValue: String? = ...
 * databaseClient.sql("...")
 *   .bind("param1", "...")
 *   .bindNull("param2", String::class.java)
 *   .bindNullable<String>("nullableParam", nullableValue) // this line
 * ```
 *
 * @author RJ
 */
inline fun <reified T> GenericExecuteSpec.bindNullable(
  name: String,
  value: Any?
): GenericExecuteSpec = value?.let { this.bind(name, it) } ?: this.bindNull(name, T::class.java)

/**
 * Extension for [Query]
 * providing a convenient method to set limit with a [Optional] value.
 *
 * Usage:
 *
 * ```
 * val limit: Optional<Int> = ...
 * entityTemplate.select<X>()
 *   .from(TABLE_X)
 *   .matching(
 *     query(criteria)
 *       .offset(offset)
 *       .limit(limit) // this line
 *     )
 *   )
 * ```
 *
 * @author RJ
 */
fun Query.limit(limit: Optional<Int>): Query {
  return if (limit.isPresent) this.limit(limit.get())
  else this
}

/**
 * Extension for [GenericExecuteSpec] to bind multiple not null values.
 */
fun GenericExecuteSpec.bind(nameValues: Map<String, Any>): GenericExecuteSpec {
  var spec = this
  nameValues.forEach { spec = spec.bind(it.key, it.value) }
  return spec
}

/**
 * Extension for [GenericExecuteSpec] to bind multiple null values.
 */
fun GenericExecuteSpec.bindNull(nameTypes: Map<String, Class<*>>): GenericExecuteSpec {
  var spec = this
  nameTypes.forEach { spec = spec.bindNull(it.key, it.value) }
  return spec
}

/**
 * Extension for [DatabaseClient] to insert by entity and return its custom-id or auto-generated-id.
 *
 * @param entity the data holder
 * @param table the table name
 * @param autoGenerateId whether the insertion use the auto-generate-id strategy, default is true
 * @param includeNullValue whether to insert with null value property, default is false
 * @param excludeNames all the property name to exclude, default is empty list
 * @param nameConverter the lambda for convert the property name to the table column name,
 *        default convert the property name to underscore name and use it as column name
 * @param valueConverter the lambda for convert the property value to the table column value,
 *        default use the property value as column value
 */
inline fun <reified T : Id<I>, reified I : Any> DatabaseClient.insert(
  table: String,
  entity: T,
  autoGenerateId: Boolean = true,
  includeNullValue: Boolean = false,
  excludeNames: List<String> = emptyList(),
  noinline nameConverter: (name: String) -> String = { name -> name },
  valueConverter: (name: String, value: Any) -> Any = { _, value -> value }
): Mono<I> {
  // 1. collect property name-value-type
  val nameValueTypes: List<Triple<String, Any?, KClass<*>>> = T::class.memberProperties
    .filter {
      it.visibility == KVisibility.PUBLIC                 // only public properties
        && !excludeNames.contains(it.name)                // exclude specific property
        && if (it.name == "id") !autoGenerateId else true // exclude id property
    }.map { Triple(it.name, it.get(entity), it.returnType.classifier as KClass<*>) }
    .filterNot { !includeNullValue && it.second == null } // exclude null value property

  // 2. generate the insert SQL from entity properties
  val nameValues = nameValueTypes.map { it.first to it.second }
  val sql = """
    insert into $table (
      ${nameValues.joinToString(", ") { underscore(nameConverter(it.first)) }}
    ) values (
      ${nameValues.joinToString(", ") { ":${it.first}" }}
    )""".trimIndent()

  // 3. bing entity property value
  var spec = this.sql(sql)
  nameValueTypes
    // bind value with name as sql param marker
    .forEach { p ->
      spec =
        if (p.second != null) spec.bind(p.first, valueConverter(p.first, p.second!!)) // bind not null value
        else spec.bindNull(p.first, p.third.javaObjectType)                           // bind null value
    }

  // 4. execute and return id
  return if (!autoGenerateId) spec.then().thenReturn(entity.id) // return the custom id
  else spec.filter { s -> s.returnGeneratedValues("id") }
    .map { row: Row -> row.get("id") as I }                     // get auto generated id
    .one()
}

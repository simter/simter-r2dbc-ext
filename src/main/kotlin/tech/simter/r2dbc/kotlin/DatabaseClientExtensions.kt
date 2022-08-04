package tech.simter.r2dbc.kotlin

import org.springframework.data.relational.core.query.Query
import org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec
import java.util.*

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

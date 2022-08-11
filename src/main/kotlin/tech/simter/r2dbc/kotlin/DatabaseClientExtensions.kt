package tech.simter.r2dbc.kotlin

import io.r2dbc.spi.Row
import org.springframework.data.relational.core.query.Query
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import tech.simter.kotlin.data.Id
import tech.simter.util.StringUtils.underscore
import java.util.*
import java.util.function.Function
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

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
  name: String, value: Any?
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
 * @param nameMapper the specific mapper for convert the property-name to the table-column-name,
 *        default use the underscore state of the property-name as the table-column-name
 * @param valueMapper the specific mapper for convert the property-value to the table-column-value,
 *        default use the property-value as the table-column-value
 */
inline fun <reified T : Id<I>, reified I : Any> DatabaseClient.insert(
  table: String,
  entity: T,
  autoGenerateId: Boolean = true,
  includeNullValue: Boolean = false,
  excludeNames: List<String> = emptyList(),
  nameMapper: Map<String, String> = emptyMap(),
  valueMapper: Map<String, (value: Any?) -> Any?> = emptyMap()
): Mono<I> {
  // 1. collect property name-value-type
  val nameValueTypes: List<Triple<String, Any?, KClass<*>>> = T::class.memberProperties.filter {
    it.visibility == KVisibility.PUBLIC                 // only public properties
      && !excludeNames.contains(it.name)                // exclude specific property
      && if (it.name == "id") !autoGenerateId else true // exclude id property
  }.map { Triple(it.name, it.get(entity), it.returnType.classifier as KClass<*>) }
    .filterNot { !includeNullValue && it.second == null } // exclude null value property

  // 2. generate the insert SQL from entity properties
  val nameValues = nameValueTypes.map { it.first to it.second }
  val sql = """
    insert into $table (
      ${nameValues.joinToString(", ") { if (nameMapper.contains(it.first)) nameMapper[it.first]!! else underscore(it.first) }}
    ) values (
      ${nameValues.joinToString(", ") { ":${it.first}" }}
    )""".trimIndent()

  // 3. bing entity property value
  var spec = this.sql(sql)
  nameValueTypes
    // bind value with property-name as sql-param-marker
    .forEach { p ->
      val value = if (valueMapper.contains(p.first)) valueMapper[p.first]!!.invoke(p.second) else p.second
      spec = if (value != null) spec.bind(p.first, value) // bind not null value
      else spec.bindNull(p.first, p.third.javaObjectType) // bind null value
    }

  // 4. execute and return id
  return if (!autoGenerateId) spec.then().thenReturn(entity.id) // return the custom id
  else spec.filter { s -> s.returnGeneratedValues("id") }
    .map { row: Row -> row.get("id") as I }                     // get auto generated id
    .one()
}

/**
 * Extension for [DatabaseClient] to select sql to entity.
 *
 * @param sql the query sql
 * @param params the param with not null value to bind, default is empty
 * @param excludeNames all the column name to exclude, default is empty list
 * @param nameMapper the specific mapper for convert the constructor-param-name to the table-column-name,
 *        default use the underscore and uppercase state of the constructor-param-name as the table-column-name
 * @param valueMapper the specific mapper for convert the table-column-value to the constructor-param-value,
 *        default use the table-column-value as the constructor-param-value
 */
inline fun <reified T : Any> DatabaseClient.select(
  sql: String,
  params: Map<String, Any> = emptyMap(),
  excludeNames: List<String> = emptyList(),
  nameMapper: Map<String, String> = emptyMap(),
  valueMapper: Map<String, (value: Any?) -> Any?> = emptyMap()
): Flux<T> {
  val kClass: KClass<T> = T::class

  // safe check
  if (!kClass.isData) return Flux.error(UnsupportedOperationException("Only support data class now"))
  val constructor = kClass.primaryConstructor
    ?: return Flux.error(UnsupportedOperationException("Could not find a primary constructor from ${kClass.qualifiedName}"))
  if (constructor.parameters.isEmpty()) return Flux.error(UnsupportedOperationException("Could not find any primary constructor parameters from ${kClass.qualifiedName}"))

  // cache the parameter and table-column-name pair
  val parameters: Map<KParameter, String> =
    constructor.parameters.filterNot { excludeNames.contains(it.name) } // exclude specific parameter
      .associateWith {
        // The column name of database return is always uppercase, so need to uppercase the final result
        when {
          nameMapper.contains(it.name!!) -> nameMapper[it.name!!]!!.uppercase()
          else -> underscore(it.name!!).uppercase()// default use the parameter name
        }
      }

  return this.sql(sql).bind(params)
    // convert row to entity instance
    .map { row: Row ->
      // get constructor parameter value from row
      val args = mutableMapOf<KParameter, Any?>()
      parameters.forEach {
        if (row.metadata.contains(it.value)) {
          //println("paramName=${it.key.name}, columnName=${it.value}, columnType=${(it.key.type.classifier as KClass<*>).javaObjectType.name}")
          args[it.key] = if (valueMapper.contains(it.key.name)) valueMapper[it.key.name]!!.invoke(row.get(it.value))
          else row.get(it.value, (it.key.type.classifier as KClass<*>).javaObjectType)
        }
      }

      // call constructor to create an entity instance
      constructor.callBy(args)
    }.all()
}

/**
 * Only emit the first element from [DatabaseClient.select].
 *
 * All params are the same with [DatabaseClient.select].
 */
inline fun <reified T : Any> DatabaseClient.selectFirstRow(
  sql: String,
  params: Map<String, Any> = emptyMap(),
  excludeNames: List<String> = emptyList(),
  nameMapper: Map<String, String> = emptyMap(),
  valueMapper: Map<String, (value: Any?) -> Any?> = emptyMap()
): Mono<T> {
  return this.select<T>(
    sql = sql,
    params = params,
    excludeNames = excludeNames,
    nameMapper = nameMapper,
    valueMapper = valueMapper,
  ).toMono()
}

/**
 * Extension for [DatabaseClient] to select a single column value to the specific type [T].
 *
 * > Note: Could not directly return null value.
 * > If you need null value return, need to use the [valueMapper] warp it to an empty [Optional] value.
 *
 * @param sql the query sql
 * @param params the param with not null value to bind, default is empty
 * @param valueMapper the specific mapper for convert the table-column-value to the return-value,
 *        default use the table-column-value as the return-value
 */
inline fun <reified T : Any> DatabaseClient.selectFirstColumn(
  sql: String,
  params: Map<String, Any> = emptyMap(),
  valueMapper: Function<Any?, T>? = null
): Flux<T> {
  return this.sql(sql).bind(params)
    .map { row -> valueMapper?.apply(row.get(0)) ?: row.get(0, T::class.javaObjectType) }
    .all() as Flux<T>
}

/**
 * Extension for [DatabaseClient] to select sql to entity.
 *
 * @param table the table name
 * @param sets the column-value pairs to set. Return [IllegalArgumentException] if it is empty
 * @param whereSql the sql part after the set sql ('update $table set ...'), default is empty
 * @param whereParams the param with not null value to bind for [whereSql], default is empty
 * @param nameMapper the specific mapper for convert the key in [sets] to the table-column-name,
 *        default use the underscore state of the key in [sets] as the table-column-name
 * @param valueMapper the specific mapper for convert the value in [sets] to the table-column-value,
 *        default use the value in [sets] as the table-column-value
 * @param nullValueTypes the param-value type when the value is null, the key is the param name, default is empty
 */
fun DatabaseClient.update(
  table: String,
  sets: Map<String, Any?>,
  whereSql: String = "",
  whereParams: Map<String, Any> = emptyMap(),
  nameMapper: Map<String, String> = emptyMap(),
  valueMapper: Map<String, (value: Any?) -> Any?> = emptyMap(),
  nullValueTypes: Map<String, Class<*>> = emptyMap(),
): Mono<Int> {
  // safe check
  if (sets.isEmpty()) return Mono.error(IllegalArgumentException("Param 'sets' could not be empty"))

  // create sql
  val sql = """
    update $table
      set ${sets.keys.joinToString(", ") { "${underscore(nameMapper.getOrDefault(it, it))} = :$it" }}
      $whereSql
    """.trimIndent()

  // do update
  val bindParams: Map<String, Any?> = (sets + whereParams)
    .mapValues { if (valueMapper.contains(it.key)) valueMapper[it.key]!!.invoke(it.value) else it.value }
  @Suppress("UNCHECKED_CAST")
  return this.sql(sql)
    .bind(bindParams.filterNot { it.value == null } as Map<String, Any>)
    .bindNull(bindParams.filter { it.value == null }.mapValues {
      nullValueTypes[it.key]
        ?: error("Param '${it.key}' has a null mapped value but missing type config, can use 'nullValueTypes' to config it")
    })
    .fetch().rowsUpdated()
}

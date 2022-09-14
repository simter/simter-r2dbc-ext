package tech.simter.r2dbc.kotlin

import io.r2dbc.spi.Row
import org.springframework.data.relational.core.query.Query
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import tech.simter.kotlin.data.Id
import tech.simter.util.StringUtils
import tech.simter.util.StringUtils.underscore
import java.util.*
import java.util.function.Function
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
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
 * @param externalColumnValues the external table-column-value add to the insert operation, default is empty map
 */
inline fun <reified T : Id<I>, reified I : Any> DatabaseClient.insert(
  table: String,
  entity: T,
  autoGenerateId: Boolean = true,
  includeNullValue: Boolean = false,
  excludeNames: List<String> = emptyList(),
  nameMapper: Map<String, String> = emptyMap(),
  valueMapper: Map<String, (value: Any?) -> Any?> = emptyMap(),
  externalColumnValues: Map<String, Any> = emptyMap(),
): Mono<I> {
  // 1. collect property name-value-type
  val nameValueTypes: List<Triple<String, Any?, KClass<*>>> = T::class.memberProperties.filter {
    it.visibility == KVisibility.PUBLIC                 // only public properties
      && !excludeNames.contains(it.name)                // exclude specific property
      && if (it.name == "id") !autoGenerateId else true // exclude id property
  }.map { Triple(it.name, it.get(entity), it.returnType.classifier as KClass<*>) }
    .filterNot { !includeNullValue && it.second == null } // exclude null value property

  // 2. generate the insert SQL from entity properties
  val names = externalColumnValues.map { it.key } + nameValueTypes.map { it.first }
  val sql = """
    insert into $table (
      ${names.joinToString(", ") { if (nameMapper.contains(it)) nameMapper[it]!! else underscore(it) }}
    ) values (
      ${names.joinToString(", ") { ":${it}" }}
    )""".trimIndent()

  // 3. bing entity property value
  var spec = this.sql(sql)

  // bind external column-value
  externalColumnValues.forEach { (k, v) -> spec = spec.bind(k, v) }

  // bind entity property-value
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
 * Extension for [DatabaseClient] to batch insert by entity and return its custom-id or auto-generated-id.
 *
 * @param entities the data holder
 * @param table the table name
 * @param autoGenerateId whether the insertion use the auto-generate-id strategy, default is true
 * @param excludeNames all the property name to exclude, default is empty list
 * @param nameMapper the specific mapper for convert the property-name to the table-column-name,
 *        default use the underscore state of the property-name as the table-column-name
 * @param valueMapper the specific mapper for convert the property-value to the table-column-value,
 *        default use the property-value as the table-column-value
 * @param externalColumnValues the external table-column-value add to the insert operation, default is empty map
 */
inline fun <reified T : Id<I>, reified I : Any> DatabaseClient.batchInsert(
  table: String,
  entities: List<T>,
  autoGenerateId: Boolean = true,
  excludeNames: List<String> = emptyList(),
  nameMapper: Map<String, String> = emptyMap(),
  valueMapper: Map<String, (value: Any?) -> Any?> = emptyMap(),
  externalColumnValues: Map<String, Any> = emptyMap(),
): Mono<List<I>> {
  if (entities.isEmpty()) return Mono.just(emptyList())

  // 1. collect property name-kProperty-type
  val namePropertyTypes: List<Triple<String, KProperty1<T, *>, KClass<*>>> = T::class.memberProperties.filter {
    it.visibility == KVisibility.PUBLIC                 // only public properties
      && !excludeNames.contains(it.name)                // exclude specific property
      && if (it.name == "id") !autoGenerateId else true // exclude id property
  }.map { Triple(it.name, it, it.returnType.classifier as KClass<*>) }

  // 2. generate the insert SQL from entity properties
  // insert into t(...) values (:p0, ...), (:p1, ...), ...
  val names = externalColumnValues.map { it.key } + namePropertyTypes.map { it.first }
  val sql = """
      insert into $table (
         ${names.joinToString(", ") { if (nameMapper.contains(it)) nameMapper[it]!! else underscore(it) }}
       ) values 
         ${
    List(entities.size) { i ->
      names.joinToString(
        prefix = "(",
        separator = ", ",
        postfix = ")"
      ) { if (externalColumnValues.containsKey(it)) ":${it}" else ":${it}$i" }
    }.joinToString(",\r\n         ")
  }
    """.trimIndent()

  // 3. bing entity property value
  var spec = this.sql(sql)

  // bind external column-value
  externalColumnValues.forEach { (k, v) -> spec = spec.bind(k, v) }

  // bind entity property-value
  List(entities.size) { i ->
    namePropertyTypes
      // bind value with property-name-rowIndex as sql-param-marker
      .forEach { p ->
        val value =
          if (valueMapper.contains(p.first)) valueMapper[p.first]!!.invoke(p.second.get(entities[i])) else p.second.get(
            entities[i]
          )
        spec = if (value != null) spec.bind("${p.first}$i", value) // bind not null value
        else spec.bindNull("${p.first}$i", p.third.javaObjectType) // bind null value
      }
  }
  // 4. execute and return id
  return if (!autoGenerateId) spec.then().thenReturn(entities.map { it.id }) // return the custom id
  else spec.filter { s -> s.returnGeneratedValues("id") }
    .map { row: Row -> row.get("id") as I }                     // get auto generated id
    .all()
    .collectList()
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
 * Extension for [DatabaseClient] to select sql to `Flux<Map<String, Any?>>`.
 *
 * @param sql the query sql
 * @param params the param with not null value to bind, default is empty
 * @param valueTypes specific the return value java type, the key is the return Map's key
 * @param nameMapper the specific mapper for convert the alias-name to the return Map's key,
 *        default use the came-case state of the alias-name as the return Map's key
 * @param valueMapper the specific mapper for convert the table-column-value to the Map's value,
 *        default is empty and no mapper
 */
inline fun DatabaseClient.selectToMap(
  sql: String,
  params: Map<String, Any> = emptyMap(),
  valueTypes: Map<String, Class<*>>,
  crossinline nameMapper: (String) -> String = StringUtils::camelcase,
  valueMapper: Map<String, (value: Any?) -> Any?> = emptyMap(),
): Flux<Map<String, Any?>> {
  return this.sql(sql).bind(params)
    .map { row: Row ->
      // convert row to map
      // notes: the alias name of database return is always uppercase
      row.metadata.columnMetadatas.associate {
        val mappedName = nameMapper.invoke(it.name)
        mappedName to if (valueMapper.contains(mappedName)) valueMapper[mappedName]!!.invoke(row.get(it.name))
        else {
          row.get(
            it.name,
            valueTypes[mappedName]
              ?: throw IllegalArgumentException("Missing value-type config for name '${mappedName}'")
          )
        }
      }
    }.all()
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
 * @return the updated count
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

/**
 * Extension for [DatabaseClient] to judge whether the sql execution return any value.
 *
 * @param sql the query sql
 * @param params the param with not null value to bind, default is empty
 */
fun DatabaseClient.exists(
  sql: String,
  params: Map<String, Any> = emptyMap(),
): Mono<Boolean> {
  return this.sql(sql)
    .bind(params)
    .fetch()
    .first()
    .hasElement()
}

/**
 * Extension for [DatabaseClient] to execute deletion.
 *
 * @param sql the delete sql
 * @param params the param with not null value to bind, default is empty
 * @return the deleted count
 */
fun DatabaseClient.delete(
  sql: String,
  params: Map<String, Any> = emptyMap(),
): Mono<Int> {
  return this.sql(sql)
    .bind(params)
    .fetch().rowsUpdated()
}

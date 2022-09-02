package tech.simter.r2dbc.kotlin.databaseclient

import tech.simter.kotlin.data.Id
import java.time.LocalDate

data class Sample2(
  override val id: Int = 0,
  val ts: LocalDate,
  val theName: String? = null,
  val createBy: String? = null,
  val status: Status = Status.Second,
) : Id<Int> {
  enum class Status {
    First, Second
  }

  companion object {
    val STATUS_2_DB_VALUE: Pair<String, (value: Any?) -> Any?> = "status" to { (it as Status).name }
    val DB_2_STATUS_VALUE: Pair<String, (value: Any?) -> Any?> = "status" to { Status.valueOf(it as String) }
  }
}
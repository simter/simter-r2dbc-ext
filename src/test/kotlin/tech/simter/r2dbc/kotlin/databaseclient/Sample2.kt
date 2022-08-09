package tech.simter.r2dbc.kotlin.databaseclient

import tech.simter.kotlin.data.Id
import java.time.LocalDate

data class Sample2(
  override val id: Int = 0,
  val ts: LocalDate,
  val theName: String? = null,
  val createBy: String? = null,
) : Id<Int>
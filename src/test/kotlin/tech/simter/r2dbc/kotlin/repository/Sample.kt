package tech.simter.r2dbc.kotlin.repository

import org.springframework.data.annotation.Id
import org.springframework.data.domain.Persistable

data class Sample(
  @JvmField
  @Id val id: Int,
  val name: String
) : Persistable<Int> {
  override fun isNew(): Boolean {
    return true
  }

  override fun getId(): Int {
    return id
  }
}
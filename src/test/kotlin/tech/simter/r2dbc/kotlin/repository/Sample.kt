package tech.simter.r2dbc.kotlin.repository

import org.springframework.data.annotation.Id

data class Sample(
  @Id override val id: Int,
  val name: String
) : tech.simter.kotlin.data.Id<Int>
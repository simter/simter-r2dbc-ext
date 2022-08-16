package tech.simter.r2dbc.kotlin.repository

import org.springframework.data.annotation.Id

data class Sample(
  @Id val id: Int,
  val name: String
)
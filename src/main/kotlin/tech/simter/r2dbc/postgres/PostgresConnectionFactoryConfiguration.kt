package tech.simter.r2dbc.postgres

import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import io.r2dbc.spi.ConnectionFactory
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tech.simter.r2dbc.R2dbcProperties

@Configuration
@ConditionalOnClass(name = ["io.r2dbc.postgresql.PostgresqlConnectionConfiguration"])
open class PostgresConnectionFactoryConfiguration @Autowired constructor(
  private val properties: R2dbcProperties
) {
  private val logger = LoggerFactory.getLogger(PostgresConnectionFactoryConfiguration::class.java)

  init {
    if (logger.isDebugEnabled) logger.debug("R2dbcProperties={}", properties.copy(password = "***"))
  }

  @Bean
  open fun connectionFactory(): ConnectionFactory {
    val builder = PostgresqlConnectionConfiguration.builder()
    properties.name?.let { builder.database(it) }
    properties.host?.let { builder.host(it) }
    properties.username?.let { builder.username(it) }
    properties.password?.let { builder.password(it) }
    properties.port?.let { builder.port(it) }
    return PostgresqlConnectionFactory(builder.build())
  }
}
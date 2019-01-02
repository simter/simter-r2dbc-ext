package tech.simter.r2dbc.mssql

import io.r2dbc.mssql.MssqlConnectionConfiguration
import io.r2dbc.mssql.MssqlConnectionFactory
import io.r2dbc.spi.ConnectionFactory
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tech.simter.r2dbc.R2dbcProperties

@Configuration
@ConditionalOnClass(name = ["io.r2dbc.mssql.MssqlConnectionConfiguration"])
open class MssqlConnectionFactoryConfiguration @Autowired constructor(
  private val properties: R2dbcProperties
) {
  private val logger = LoggerFactory.getLogger(MssqlConnectionFactoryConfiguration::class.java)

  init {
    if (logger.isWarnEnabled) logger.warn("R2dbcProperties={}", properties.copy(password = "***"))
  }

  @Bean
  open fun connectionFactory(): ConnectionFactory {
    val builder = MssqlConnectionConfiguration.builder()
    properties.name?.let { builder.database(it) }
    properties.host?.let { builder.host(it) }
    properties.username?.let { builder.username(it) }
    properties.password?.let { builder.password(it) }
    properties.port?.let { builder.port(it) }
    return MssqlConnectionFactory(builder.build())
  }
}
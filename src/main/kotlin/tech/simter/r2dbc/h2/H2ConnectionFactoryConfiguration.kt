package tech.simter.r2dbc.h2

import io.r2dbc.h2.H2ConnectionConfiguration
import io.r2dbc.h2.H2ConnectionFactory
import io.r2dbc.spi.ConnectionFactory
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tech.simter.r2dbc.R2dbcProperties

@Configuration
@ConditionalOnClass(name = ["io.r2dbc.h2.H2ConnectionConfiguration"])
open class H2ConnectionFactoryConfiguration @Autowired constructor(
  private val properties: R2dbcProperties
) {
  private val logger = LoggerFactory.getLogger(H2ConnectionFactoryConfiguration::class.java)

  init {
    if (logger.isWarnEnabled) logger.warn("R2dbcProperties={}", properties.copy(password = "***"))
  }

  @Bean
  open fun connectionFactory(): ConnectionFactory {
    val builder = H2ConnectionConfiguration.builder()
    properties.url?.let { builder.url(it) }
    properties.username?.let { builder.username(it) }
    properties.password?.let { builder.password(it) }
    return H2ConnectionFactory(builder.build())
  }
}
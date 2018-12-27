package tech.simter.r2dbc

import io.r2dbc.spi.ConnectionFactory
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration

/**
 * @author RJ
 */
@Configuration
open class R2dbcConfiguration(private val connectionFactory: ConnectionFactory) : AbstractR2dbcConfiguration() {
  override fun connectionFactory(): ConnectionFactory {
    return this.connectionFactory
  }
}
package tech.simter.r2dbc

import io.r2dbc.spi.ConnectionFactory
import org.slf4j.LoggerFactory
import org.springframework.boot.jdbc.DataSourceInitializationMode.NEVER
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration
import org.springframework.util.FileCopyUtils
import reactor.core.publisher.Mono
import java.io.InputStreamReader
import javax.annotation.PostConstruct

/**
 * @author RJ
 */
@Configuration
open class R2dbcConfiguration(
  private val connectionFactory: ConnectionFactory,
  private val properties: R2dbcProperties
) : AbstractR2dbcConfiguration() {
  private val logger = LoggerFactory.getLogger(R2dbcConfiguration::class.java)
  override fun connectionFactory(): ConnectionFactory {
    return this.connectionFactory
  }

  @PostConstruct
  fun init() {
    // initial database by execute SQL script through R2dbcProperties.schema|initializationMode config
    val resourcePatternResolver = PathMatchingResourcePatternResolver()
    if (!properties.schema.isNullOrEmpty() && properties.initializationMode != NEVER) {
      //Flux.fromIterable(properties.schema)
      properties.schema?.forEach {
        logger.info("Executing SQL script from $it")
        val sql = FileCopyUtils.copyToString(InputStreamReader(resourcePatternResolver.getResource(it).inputStream))
        Mono.from(connectionFactory().create())
          .flatMapMany { c -> c.createStatement(sql).execute() }
          .log()
          .then()
          .subscribe()
      }
    }
  }
}
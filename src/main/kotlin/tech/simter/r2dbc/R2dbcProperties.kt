package tech.simter.r2dbc

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.DataSourceInitializationMode
import org.springframework.boot.jdbc.DataSourceInitializationMode.NEVER
import org.springframework.stereotype.Component

/**
 * See [DataSourceProperties]
 * See ['50.5 @ConfigurationProperties'](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-kotlin.html#boot-features-kotlin-configuration-properties)
 * @author RJ
 */
@Component
@ConfigurationProperties(prefix = "spring.datasource")
data class R2dbcProperties(
  var name: String? = "test",
  var host: String? = "localhost",
  var port: Int? = null,
  var username: String? = "test",
  var password: String? = "password",
  var url: String? = null, // for h2
  var initializationMode: DataSourceInitializationMode = NEVER,
  var schema: List<String>? = null
)
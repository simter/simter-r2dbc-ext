package tech.simter.r2dbc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Wither;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceInitializationMode;

import java.util.List;

/**
 * See [DataSourceProperties]
 * See ['50.5 @ConfigurationProperties'](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-kotlin.html#boot-features-kotlin-configuration-properties)
 *
 * @author RJ
 */
@ConfigurationProperties(prefix = "spring.datasource")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Wither
@Builder(toBuilder = true)
public class R2dbcProperties {
  private String platform;
  private String name;
  private String host;
  private Integer port;
  private String username;
  private String password;
  private String url; // for h2
  private DataSourceInitializationMode initializationMode;
  private List<String> schema;
  private List<String> data;
}
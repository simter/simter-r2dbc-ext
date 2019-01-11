package tech.simter.r2dbc.postgres;

import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.simter.r2dbc.R2dbcProperties;

@Configuration
@ConditionalOnClass(name = "io.r2dbc.postgresql.PostgresqlConnectionConfiguration")
public class PostgresConnectionFactoryConfiguration {
  private final static Logger logger = LoggerFactory.getLogger(PostgresConnectionFactoryConfiguration.class);
  private final R2dbcProperties properties;

  @Autowired
  public PostgresConnectionFactoryConfiguration(R2dbcProperties properties) {
    this.properties = properties;
    if (logger.isDebugEnabled()) logger.debug("R2dbcProperties={}", properties.withPassword("***"));
  }

  @Bean
  public ConnectionFactory connectionFactory() {
    PostgresqlConnectionConfiguration.Builder builder = PostgresqlConnectionConfiguration.builder();
    if (properties.getName() != null) builder.database(properties.getName());
    if (properties.getHost() != null) builder.host(properties.getHost());
    if (properties.getPort() != null) builder.port(properties.getPort());
    if (properties.getUsername() != null) builder.username(properties.getUsername());
    if (properties.getPassword() != null) builder.password(properties.getPassword());

    return new PostgresqlConnectionFactory(builder.build());
  }
}
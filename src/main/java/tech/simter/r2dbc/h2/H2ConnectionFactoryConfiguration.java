package tech.simter.r2dbc.h2;

import io.r2dbc.h2.H2ConnectionConfiguration;
import io.r2dbc.h2.H2ConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.simter.r2dbc.R2dbcProperties;

@Configuration
@ConditionalOnClass(name = "io.r2dbc.h2.H2ConnectionConfiguration")
public class H2ConnectionFactoryConfiguration {
  private final static Logger logger = LoggerFactory.getLogger(H2ConnectionFactoryConfiguration.class);
  private final R2dbcProperties properties;

  @Autowired
  public H2ConnectionFactoryConfiguration(R2dbcProperties properties) {
    this.properties = properties;
    if (logger.isDebugEnabled()) logger.debug("R2dbcProperties={}", properties);
  }

  @Bean
  public ConnectionFactory connectionFactory() {
    H2ConnectionConfiguration.Builder builder = H2ConnectionConfiguration.builder();
    if (properties.getUrl() != null) builder.url(properties.getUrl());
    if (properties.getUsername() != null) builder.username(properties.getUsername());
    if (properties.getPassword() != null) builder.password(properties.getPassword());

    return new H2ConnectionFactory(builder.build());
  }
}
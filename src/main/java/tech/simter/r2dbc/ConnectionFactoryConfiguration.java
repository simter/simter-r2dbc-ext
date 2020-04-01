package tech.simter.r2dbc;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static io.r2dbc.spi.ConnectionFactoryOptions.*;

/**
 * Auto config a r2dbc {@link ConnectionFactory} instance base on a {@link R2dbcProperties} config
 * through by property 'spring.datasource' config.
 * <p>
 * Use <a href="https://r2dbc.io/spec/0.8.0.RELEASE/spec/html/#connections.factory.discovery">R2dbc ConnectionFactory Discovery Mechanism</a>.
 *
 * @author RJ
 */
@Configuration
@ConditionalOnMissingBean(ConnectionFactory.class)
public class ConnectionFactoryConfiguration {
  private final static Logger logger = LoggerFactory.getLogger(ConnectionFactoryConfiguration.class);
  private final R2dbcProperties properties;

  @Autowired
  public ConnectionFactoryConfiguration(R2dbcProperties properties) {
    this.properties = properties;
    if (logger.isDebugEnabled()) logger.debug("R2dbcProperties={}", properties);
  }

  /**
   * See https://r2dbc.io/spec/0.8.0.RELEASE/spec/html/#connections.factory.options
   * <p>
   * 1. https://github.com/r2dbc/r2dbc-h2 <br>
   * 2. https://github.com/r2dbc/r2dbc-postgres <br>
   * 3. https://github.com/r2dbc/r2dbc-mssql <br>
   * 4. https://github.com/mirromutth/r2dbc-mysql
   *
   * @return the connectionFactory
   */
  @Bean
  @ConditionalOnMissingBean(ConnectionFactory.class)
  public ConnectionFactory connectionFactory() {
    Builder builder = ConnectionFactoryOptions.builder();
    if (properties.getProtocol() != null) builder.option(PROTOCOL, properties.getProtocol());
    if (properties.getPlatform() != null) builder.option(DRIVER, properties.getPlatform());
    if (properties.getName() != null) builder.option(DATABASE, properties.getName());
    if (properties.getHost() != null) builder.option(HOST, properties.getHost());
    if (properties.getPort() != null) builder.option(PORT, properties.getPort());
    if (properties.getUsername() != null) builder.option(USER, properties.getUsername());
    if (properties.getPassword() != null) builder.option(PASSWORD, properties.getPassword());
    if (properties.getOptions() != null) {
      properties.getOptions().forEach((key, value) ->
        builder.option(Option.valueOf(key), value)
      );
    }

    return ConnectionFactories.get(builder.build());
  }
}
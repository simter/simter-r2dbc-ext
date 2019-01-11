package tech.simter.r2dbc;

import io.r2dbc.spi.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.springframework.boot.jdbc.DataSourceInitializationMode.NEVER;

/**
 * @author RJ
 */
@Configuration
public class R2dbcConfiguration extends AbstractR2dbcConfiguration {
  private final static Logger logger = LoggerFactory.getLogger(R2dbcConfiguration.class);
  private final ConnectionFactory connectionFactory;
  private final R2dbcProperties properties;

  @Autowired
  public R2dbcConfiguration(ConnectionFactory connectionFactory, R2dbcProperties properties) {
    this.connectionFactory = connectionFactory;
    this.properties = properties;
  }

  @Override
  public ConnectionFactory connectionFactory() {
    return this.connectionFactory;
  }

  @PostConstruct
  public void init() {
    // initial database by execute SQL script through R2dbcProperties.schema|initializationMode config
    PathMatchingResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
    if (properties.getSchema() != null && !properties.getSchema().isEmpty()
      && properties.getInitializationMode() != null && properties.getInitializationMode() != NEVER
    ) {
      Flux.fromIterable(properties.getSchema())
        .map(it -> {
          try {
            return FileCopyUtils.copyToString(new InputStreamReader(resourcePatternResolver.getResource(it).getInputStream()));
          } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
          }
        })
        .doOnError(e -> logger.error(e.getMessage(), e))
        .collectList()
        .map(it -> {
          return StringUtils.collectionToDelimitedString(it, "\r\n"); // concat all sql script content
        })
        .doOnSuccess(it -> {
          if (logger.isInfoEnabled())
            logger.info("Executing SQL script from {}", StringUtils.collectionToDelimitedString(properties.getSchema(), "\r\n"));
          if (logger.isDebugEnabled()) logger.debug(it);
        })
        .flatMap(it -> Mono.from(connectionFactory().create())
          .flatMapMany(c -> c.createStatement(it).execute())
          .doOnError(e -> logger.error(e.getMessage(), e))
          .then(Mono.just(true))
        ).subscribe();
    }
  }
}
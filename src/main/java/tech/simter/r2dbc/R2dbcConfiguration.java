package tech.simter.r2dbc;

import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.util.FileCopyUtils;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.boot.jdbc.DataSourceInitializationMode.NEVER;

/**
 * @author RJ
 */
@Configuration
@ComponentScan
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
    if (properties.getInitializationMode() == null || properties.getInitializationMode() == NEVER) return;
    ResourceLoader resourcePatternResolver = new PathMatchingResourcePatternResolver();

    // initial database by execute SQL script through R2dbcProperties.schema|data config

    // 1. concat schema and data
    List<String> sqlResources = new ArrayList<>();
    if (properties.getSchema() != null) sqlResources.addAll(properties.getSchema());
    if (properties.getData() != null) sqlResources.addAll(properties.getData());
    if (sqlResources.isEmpty()) return;
    StringBuffer sql = new StringBuffer();
    for (int i = 0; i < sqlResources.size(); i++) {
      String resourcePath = sqlResources.get(i);
      logger.info("Load script from {}", resourcePath);
      sql.append("-- copy from ").append(resourcePath).append("\r\n\r\n")
        .append(loadSql(resourcePath, resourcePatternResolver));
      if (i < sqlResources.size() - 1) sql.append("\r\n\r\n");
    }

    // 2. save concatenate sql content to file
    if (logger.isInfoEnabled()) {
      File sqlFile = new File("target/" + properties.getPlatform() + ".sql");
      logger.info("Save concatenate SQL script to {}", sqlFile.getAbsolutePath());
      try {
        FileCopyUtils.copy(sql.toString().getBytes(StandardCharsets.UTF_8), sqlFile);
      } catch (IOException e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    }

    // 3. execute sql
    Mono.from(connectionFactory().create())
      .flatMapMany(c -> c.createStatement(sql.toString()).execute())
      .flatMap(Result::getRowsUpdated)
      .doOnNext(count -> logger.debug("result.getRowsUpdated={}", count))
      .doOnError(e -> logger.error(e.getMessage(), e))
      .subscribe();
  }

  private String loadSql(String resourcePath, ResourceLoader resourcePatternResolver) {
    try {
      return FileCopyUtils.copyToString(new InputStreamReader(
        resourcePatternResolver.getResource(resourcePath).getInputStream(), StandardCharsets.UTF_8
      ));
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
}
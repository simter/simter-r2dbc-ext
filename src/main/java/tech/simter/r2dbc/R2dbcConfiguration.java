package tech.simter.r2dbc;

import io.r2dbc.client.R2dbc;
import io.r2dbc.spi.ConnectionFactory;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.util.FileCopyUtils;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.boot.jdbc.DataSourceInitializationMode.NEVER;

/**
 * @author RJ
 */
@Configuration
@ComponentScan
@EnableConfigurationProperties(R2dbcProperties.class)
public class R2dbcConfiguration extends AbstractR2dbcConfiguration {
  private final static Logger logger = LoggerFactory.getLogger(R2dbcConfiguration.class);
  private final ConnectionFactory connectionFactory;
  private final R2dbcProperties properties;
  private boolean concatSqlScript;

  @Autowired
  public R2dbcConfiguration(ConnectionFactory connectionFactory, R2dbcProperties properties,
                            @Value("${spring.datasource.concat-sql-script:false}") boolean concatSqlScript) {
    this.connectionFactory = connectionFactory;
    this.properties = properties;
    this.concatSqlScript = concatSqlScript;
  }

  @Override
  public ConnectionFactory connectionFactory() {
    return this.connectionFactory;
  }

  // initial database by execute SQL script through R2dbcProperties.schema|data config
  @EventListener(ContextRefreshedEvent.class)
  public void onApplicationEvent() {
    if (properties.getInitializationMode() == null || properties.getInitializationMode() == NEVER)
      return;
    ResourceLoader resourcePatternResolver = new PathMatchingResourcePatternResolver();

    // 1. concat schema and data
    List<String> sqlResources = new ArrayList<>();
    if (properties.getSchema() != null) sqlResources.addAll(properties.getSchema());
    if (properties.getData() != null) sqlResources.addAll(properties.getData());
    if (sqlResources.isEmpty()) return;
    StringBuilder sql = new StringBuilder();
    Map<String, String> scriptContents = new LinkedHashMap<>();
    for (int i = 0; i < sqlResources.size(); i++) {
      String resourcePath = sqlResources.get(i);
      //logger.info("Load script from {}", resourcePath);
      String scriptContent = loadSql(resourcePath, resourcePatternResolver);
      scriptContents.put(resourcePath, scriptContent);
      sql.append("-- copy from ").append(resourcePath).append("\r\n\r\n")
        .append(scriptContent);
      if (i < sqlResources.size() - 1) sql.append("\r\n\r\n");
    }

    // 2. save concatenate sql content to file
    if (concatSqlScript) {
      File sqlFile = new File("target/" + properties.getPlatform() + ".sql");
      logger.info("Save concatenate SQL script to {}", sqlFile.getAbsolutePath());
      try {
        FileCopyUtils.copy(sql.toString().getBytes(StandardCharsets.UTF_8), sqlFile);
      } catch (IOException e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    }

    // 3. execute sql one by one
    logger.warn("Executing spring.datasource.schema|data scripts to database");
    new R2dbc(connectionFactory).useTransaction(handle -> {
      List<Publisher<Integer>> sources = new ArrayList<>();
      int i = 0, len = scriptContents.size();
      for (Map.Entry<String, String> e : scriptContents.entrySet()) {
        int j = ++i;
        sources.add(
          handle.execute(e.getValue())
            .doOnComplete(() -> logger.info("{}/{} Success executed script {}", j, len, e.getKey()))
            .doOnError(t -> logger.warn("{}/{} Failed executed script {}", j, len, e.getKey()))
        );
      }
      return Flux.concat(sources);
    }).block(Duration.ofSeconds(10));
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
package tech.simter.r2dbc;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
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
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.boot.jdbc.DataSourceInitializationMode.NEVER;

/**
 * A {@link AbstractR2dbcConfiguration} implementation with special features:<br>
 * <p>
 * 1. Auto init database by {@link R2dbcProperties}'s initializationMode, schema and data config. <br>
 * 2. If `spring.datasource.concat-sql-script=true`, concat all schema and data sql files
 * to a single sql file `target/{platform}.sql`.<br>
 * 3. Auto register all {@link SimterR2dbcConverter} spring bean instances for r2dbc.
 *
 * @author RJ
 */
@Configuration
@ComponentScan
@EnableConfigurationProperties(R2dbcProperties.class)
public class R2dbcConfiguration extends AbstractR2dbcConfiguration {
  private final static Logger logger = LoggerFactory.getLogger(R2dbcConfiguration.class);
  private final ObjectProvider<SimterR2dbcConverter<?, ?>> simterR2dbcConverters;
  private final ConnectionFactory connectionFactory;
  private final R2dbcProperties properties;
  private boolean concatSqlScript;

  @Autowired
  public R2dbcConfiguration(
    ObjectProvider<SimterR2dbcConverter<?, ?>> simterR2dbcConverters,
    ConnectionFactory connectionFactory,
    R2dbcProperties properties,
    @Value("${spring.datasource.concat-sql-script:false}") boolean concatSqlScript
  ) {
    this.simterR2dbcConverters = simterR2dbcConverters;
    this.connectionFactory = connectionFactory;
    this.properties = properties;
    this.concatSqlScript = concatSqlScript;
  }

  @Override
  public ConnectionFactory connectionFactory() {
    return this.connectionFactory;
  }

  @Override
  protected List<Object> getCustomConverters() {
    List<Object> customConverters = simterR2dbcConverters.stream().collect(Collectors.toList());
    if (!customConverters.isEmpty()) {
      logger.info("register custom r2dbc converters: (total {})", customConverters.size());
      customConverters.forEach(c -> logger.info("  {}", c.getClass().getCanonicalName()));
    }
    ArrayList<Object> expandConverters = new ArrayList<>();
    simterR2dbcConverters.forEach(c -> {
      if (c instanceof SimterR2dbcBiConverter)
        expandConverters.addAll(((SimterR2dbcBiConverter<?, ?>) c).getConverters());
      else
        expandConverters.add(c);
    });

    return expandConverters;
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
    Mono.from(connectionFactory.create())
      .flatMapMany(connection -> Mono.from(connection.beginTransaction())
        .thenMany(executeAllSql(connection, scriptContents))
        .delayUntil(t -> connection.commitTransaction())
        .onErrorResume(t -> Mono.from(connection.rollbackTransaction()).then(Mono.error(t)))
      )
      .blockLast(Duration.ofSeconds(10));
  }

  private Flux<Integer> executeAllSql(Connection connection, Map<String, String> allSql) {
    List<Flux<Integer>> sources = new ArrayList<>();
    int i = 0, len = allSql.size();
    for (Map.Entry<String, String> e : allSql.entrySet()) {
      int j = ++i;
      sources.add(
        executeSql(connection, e.getValue())
          .doOnComplete(() -> logger.info("{}/{} Success executed script {}", j, len, e.getKey()))
          .doOnError(t -> logger.warn("{}/{} Failed executed script {}", j, len, e.getKey()))
      );
    }
    return Flux.concat(sources);
  }

  private Flux<Integer> executeSql(Connection connection, String sql) {
    return Flux
      .from(connection.createStatement(sql).execute())
      .flatMap(Result::getRowsUpdated);
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
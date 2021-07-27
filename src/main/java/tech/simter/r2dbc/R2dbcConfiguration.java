package tech.simter.r2dbc;

import io.r2dbc.spi.ConnectionFactory;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A {@link AbstractR2dbcConfiguration} implementation with special features:<br>
 * <p>
 * Auto register all {@link SimterR2dbcConverter} spring bean instances for r2dbc.
 *
 * @author RJ
 */
@Configuration
@ComponentScan
public class R2dbcConfiguration extends AbstractR2dbcConfiguration {
  private final static Logger logger = LoggerFactory.getLogger(R2dbcConfiguration.class);
  private final ObjectProvider<SimterR2dbcConverter<?, ?>> simterR2dbcConverters;
  private final ConnectionFactory connectionFactory;

  @Autowired
  public R2dbcConfiguration(
    ObjectProvider<SimterR2dbcConverter<?, ?>> simterR2dbcConverters,
    ConnectionFactory connectionFactory
  ) {
    logger.info("init r2dbc configuration by simter");
    this.simterR2dbcConverters = simterR2dbcConverters;
    this.connectionFactory = connectionFactory;
  }

  @NotNull
  @Override
  public ConnectionFactory connectionFactory() {
    return this.connectionFactory;
  }

  @NotNull
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
}
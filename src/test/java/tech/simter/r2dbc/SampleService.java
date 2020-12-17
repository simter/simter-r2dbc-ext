package tech.simter.r2dbc;

import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
public class SampleService {
  private final R2dbcEntityTemplate entityTemplate;
  public static final Sample SAMPLE = new Sample(1, "tester");

  public SampleService(R2dbcEntityTemplate entityTemplate) {
    this.entityTemplate = entityTemplate;
  }

  public Mono<Sample> insertFailedByUniqueWithoutTransaction() {
    return entityTemplate.insert(SAMPLE)
      .then(entityTemplate.insert(SAMPLE));
  }

  @Transactional(readOnly = false)
  public Mono<Sample> insertFailedByUniqueWithTransaction() {
    return entityTemplate.insert(SAMPLE)
      .then(entityTemplate.insert(SAMPLE));
  }

  @Transactional(readOnly = true)
  public Mono<Sample> insertFailedByReadonlyTransaction() {
    return entityTemplate.insert(SAMPLE);
  }
}
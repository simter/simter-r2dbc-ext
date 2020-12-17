package tech.simter.r2dbc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;
import static tech.simter.r2dbc.SampleService.SAMPLE;

/**
 * Test Transactional.
 *
 * @author RJ
 */
@DataR2dbcTest
@SpringJUnitConfig(UnitTestConfiguration.class)
public class SampleServiceTransactionTest {
  @Autowired
  private R2dbcEntityTemplate entityTemplate;
  @Autowired
  private SampleService service;

  private Mono<Sample> getSample() {
    return entityTemplate.selectOne(query(where("id").is(SAMPLE.getId())), Sample.class);
  }

  @BeforeEach
  public void clear() {
    entityTemplate.delete(SAMPLE).block();
  }

  @Test
  public void failedByUniqueWithTransaction() {
    // insert failed by transaction rollback
    StepVerifier.create(service.insertFailedByUniqueWithTransaction())
      .expectError()
      .verify();

    // verify not insert
    StepVerifier.create(getSample()).verifyComplete();
  }

  /**
   * 2020-12-17 RJ: Failed on spring-boot-2.4.1
   */
  @Disabled
  @Test
  public void failedByReadonlyTransaction() {
    StepVerifier.create(service.insertFailedByReadonlyTransaction())
      .expectError()
      .verify();
  }

  @Test
  public void withoutTransaction() {
    // insert: success one and failed one
    StepVerifier.create(service.insertFailedByUniqueWithoutTransaction())
      .expectError()
      .verify();

    // verify the success one
    StepVerifier.create(
      entityTemplate.selectOne(query(where("id").is(SAMPLE.getId())), Sample.class)
    ).expectNext(SAMPLE)
      .verifyComplete();
  }
}
package tech.simter.r2dbc;

import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface SampleRepository extends R2dbcRepository<Sample, String> {
}
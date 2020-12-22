package tech.simter.r2dbc

import org.springframework.data.r2dbc.repository.R2dbcRepository

interface SampleRepository : R2dbcRepository<Sample, String>
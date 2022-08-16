package tech.simter.r2dbc.kotlin.repository

import org.springframework.data.r2dbc.repository.R2dbcRepository

interface SampleRepository : R2dbcRepository<Sample, Int>
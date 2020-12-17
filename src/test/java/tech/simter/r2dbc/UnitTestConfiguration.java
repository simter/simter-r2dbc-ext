package tech.simter.r2dbc;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * All configuration for this module.
 *
 * @author RJ
 */
@Configuration
@EnableR2dbcRepositories
@ComponentScan("tech.simter")
public class UnitTestConfiguration {
}
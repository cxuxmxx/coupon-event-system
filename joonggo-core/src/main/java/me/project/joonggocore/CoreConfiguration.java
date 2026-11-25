package me.project.joonggocore;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EntityScan(basePackageClasses = CoreConfiguration.class)
@EnableJpaRepositories(basePackageClasses = CoreConfiguration.class)
@EnableJpaAuditing
@ComponentScan
@EnableAutoConfiguration
public class CoreConfiguration {
}

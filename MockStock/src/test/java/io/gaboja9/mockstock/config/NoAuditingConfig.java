package io.gaboja9.mockstock.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

@TestConfiguration
public class NoAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return Optional::empty;
    }
}

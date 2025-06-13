package org.envyw.dadmarketplace.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;

import java.time.LocalDateTime;
import java.util.Optional;

@Configuration
@EnableR2dbcAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class R2dbcConfig {
    @Bean
    public DateTimeProvider auditingDateTimeProvider() {
        return () -> Optional.of(LocalDateTime.now());
    }
}

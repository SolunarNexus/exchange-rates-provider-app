package com.shipmonk.testingday.config;

import org.springframework.boot.validation.autoconfigure.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ValidationConfig {

    @Bean
    public ValidationConfigurationCustomizer utcClockProvider() {
        return configuration -> configuration.clockProvider(Clock::systemUTC);
    }

}

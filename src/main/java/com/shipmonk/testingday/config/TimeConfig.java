package com.shipmonk.testingday.config;

import org.springframework.boot.validation.autoconfigure.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfig {

    /**
     * The single source of the current time in UTC.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * Makes temporal constraints such as {@code @PastOrPresent} use the clock above
     * instead of Bean Validation's own system clock.
     */
    @Bean
    public ValidationConfigurationCustomizer validationClockProvider(Clock clock) {
        return configuration -> configuration.clockProvider(() -> clock);
    }

}

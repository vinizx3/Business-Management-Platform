package com.pmei.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Configuration class responsible for providing time-related beans.
 *
 * Exposes a Clock bean to centralize time handling across the application,
 * improving testability and consistency when working with dates and times.
 */
@Configuration
public class TimeConfig {

    /**
     * Provides a system default clock instance.
     *
     * Using Clock instead of directly calling LocalDateTime.now() allows
     * easier testing (e.g., mocking time in unit tests).
     *
     * @return system default Clock instance
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}

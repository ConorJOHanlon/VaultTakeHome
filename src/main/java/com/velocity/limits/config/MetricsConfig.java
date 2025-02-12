package com.velocity.limits.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {
    
    @Bean
    public Counter loadRequestCounter(MeterRegistry registry) {
        return Counter.builder("load_requests_total")
            .description("Total number of load requests processed")
            .tag("type", "request")
            .register(registry);
    }

    @Bean
    public Counter loadAmountTotal(MeterRegistry registry) {
        return Counter.builder("load_amount_total")
            .description("Total amount of loads processed")
            .tag("type", "amount")
            .register(registry);
    }

    @Bean
    public Gauge dailyLoadCount(MeterRegistry registry) {
        return Gauge.builder("daily_load_count", () -> 0)
            .description("Current number of loads for the day")
            .tag("type", "count")
            .register(registry);
    }
} 
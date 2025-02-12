package com.velocity.limits.service;

import com.velocity.limits.entity.CustomerLoad;
import com.velocity.limits.model.LoadRequest;
import com.velocity.limits.model.LoadResponse;
import com.velocity.limits.repository.CustomerLoadRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.MDC;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class LoadLimitService {
    private static final Logger log = LoggerFactory.getLogger(LoadLimitService.class);

    @Value("${load.limits.daily-amount}")
    private BigDecimal dailyLimit;

    @Value("${load.limits.weekly-amount}")
    private BigDecimal weeklyLimit;

    @Value("${load.limits.daily-count}")
    private int dailyLoadLimit;

    private final CustomerLoadRepository loadRepository;
    private final MeterRegistry meterRegistry;

    private Counter loadAttemptsCounter;
    private Counter loadAcceptedCounter;
    private Counter loadRejectedCounter;
    private Counter duplicateLoadsCounter;
    private Counter dailyLimitExceededCounter;
    private Counter weeklyLimitExceededCounter;
    private Counter dailyCountExceededCounter;
    private Timer loadProcessingTimer;

    @PostConstruct
    public void initMetrics() {
        loadAttemptsCounter = Counter.builder("load.attempts.total")
            .description("Total number of load attempts")
            .register(meterRegistry);

        loadAcceptedCounter = Counter.builder("load.accepted.total")
            .description("Total number of accepted loads")
            .register(meterRegistry);

        loadRejectedCounter = Counter.builder("load.rejected.total")
            .description("Total number of rejected loads")
            .register(meterRegistry);

        duplicateLoadsCounter = Counter.builder("load.duplicates.total")
            .description("Total number of duplicate load attempts")
            .register(meterRegistry);

        dailyLimitExceededCounter = Counter.builder("load.daily.limit.exceeded.total")
            .description("Number of times daily amount limit was exceeded")
            .register(meterRegistry);

        weeklyLimitExceededCounter = Counter.builder("load.weekly.limit.exceeded.total")
            .description("Number of times weekly amount limit was exceeded")
            .register(meterRegistry);

        dailyCountExceededCounter = Counter.builder("load.daily.count.exceeded.total")
            .description("Number of times daily load count limit was exceeded")
            .register(meterRegistry);

        loadProcessingTimer = Timer.builder("load.processing.time")
            .description("Time taken to process load requests")
            .register(meterRegistry);
    }

    @Timed(value = "load.process.time", description = "Time taken to process load request")
    @Transactional
    public LoadResponse processLoad(LoadRequest request) {
        return loadProcessingTimer.record(() -> {
            try {
                MDC.put("customerId", request.getCustomerId());
                MDC.put("loadId", request.getId());
                
                loadAttemptsCounter.increment();
                log.debug("Processing load request: id={}, customer={}, amount={}, time={}", 
                    request.getId(), request.getCustomerId(), request.getLoadAmount(), request.getTime());

                if (loadRepository.existsByLoadIdAndCustomerId(request.getId(), request.getCustomerId())) {
                    duplicateLoadsCounter.increment();
                    return null;
                }

                boolean accepted = checkLimits(request);
                
                if (accepted) {
                    loadAcceptedCounter.increment();
                    saveLoad(request);
                } else {
                    loadRejectedCounter.increment();
                }

                return LoadResponse.builder()
                        .id(request.getId())
                        .customerId(request.getCustomerId())
                        .accepted(accepted)
                        .build();
            } finally {
                MDC.clear();
            }
        });
    }

    private boolean checkLimits(LoadRequest request) {
        ZonedDateTime loadTime = request.getTime();
        ZonedDateTime startOfDay = loadTime.toLocalDate().atStartOfDay(loadTime.getZone());
        ZonedDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);
        
        // Get start and end of week (Monday to Sunday)
        ZonedDateTime startOfWeek = loadTime
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .truncatedTo(ChronoUnit.DAYS);
        ZonedDateTime endOfWeek = startOfWeek
                .plusDays(7)
                .minusNanos(1);

        log.debug("Checking limits for time={}, startOfWeek={}, endOfWeek={}", 
            loadTime, startOfWeek, endOfWeek);

        // Check daily load count
        long dailyLoadCount = loadRepository.countByCustomerIdAndLoadTimeBetween(
                request.getCustomerId(), startOfDay, endOfDay);
        if (dailyLoadCount >= dailyLoadLimit) {
            dailyCountExceededCounter.increment();
            log.debug("Daily load count limit exceeded: customer={}, count={}", 
                request.getCustomerId(), dailyLoadCount);
            return false;
        }

        // Check daily amount
        BigDecimal dailyTotal = loadRepository.sumAmountByCustomerIdAndLoadTimeBetween(
                request.getCustomerId(), startOfDay, endOfDay);
        dailyTotal = dailyTotal == null ? BigDecimal.ZERO : dailyTotal;
        if (dailyTotal.add(request.getLoadAmountValue()).compareTo(dailyLimit) > 0) {
            dailyLimitExceededCounter.increment();
            log.debug("Daily amount limit exceeded: customer={}, current={}, attempted={}", 
                request.getCustomerId(), dailyTotal, request.getLoadAmount());
            return false;
        }

        // Check weekly amount
        BigDecimal weeklyTotal = loadRepository.sumAmountByCustomerIdAndLoadTimeBetween(
                request.getCustomerId(), startOfWeek, endOfWeek);
        weeklyTotal = weeklyTotal == null ? BigDecimal.ZERO : weeklyTotal;
        if (weeklyTotal.add(request.getLoadAmountValue()).compareTo(weeklyLimit) > 0) {
            weeklyLimitExceededCounter.increment();
            log.debug("Weekly amount limit exceeded: customer={}, current={}, attempted={}", 
                request.getCustomerId(), weeklyTotal, request.getLoadAmount());
            return false;
        }

        return true;
    }

    private void saveLoad(LoadRequest request) {
        try {
            CustomerLoad load = new CustomerLoad();
            load.setLoadId(request.getId());
            load.setCustomerId(request.getCustomerId());
            load.setAmount(request.getLoadAmountValue());
            load.setLoadTime(request.getTime());
            loadRepository.save(load);
            log.debug("Load saved successfully: id={}, customer={}", 
                request.getId(), request.getCustomerId());
        } catch (Exception e) {
            log.error("Error saving load: id={}, customer={}", 
                request.getId(), request.getCustomerId(), e);
            throw new RuntimeException("Failed to save load", e);
        }
    }
} 
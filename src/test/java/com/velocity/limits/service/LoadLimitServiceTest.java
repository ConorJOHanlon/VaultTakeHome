package com.velocity.limits.service;

import com.velocity.limits.model.LoadRequest;
import com.velocity.limits.model.LoadResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class LoadLimitServiceTest {

    @Autowired
    private LoadLimitService loadLimitService;

    @Value("${load.limits.daily-amount}")
    private BigDecimal dailyLoadLimit;

    @Value("${load.limits.weekly-amount}")
    private BigDecimal weeklyLoadLimit;

    @Value("${load.limits.daily-count}")
    private int maxDailyLoads;

    private String formatAmount(BigDecimal amount) {
        return "$" + amount.setScale(2).toString();
    }

    private LoadRequest createLoadRequest(String id, String customerId, String amount, String time) {
        LoadRequest request = new LoadRequest();
        request.setId(id);
        request.setCustomerId(customerId);
        request.setLoadAmount(amount);
        request.setTime(ZonedDateTime.parse(time));
        return request;
    }

    @Test
    void shouldAcceptLoadUnderDailyLimit() {
        // Use 80% of daily limit
        BigDecimal amount = dailyLoadLimit.multiply(new BigDecimal("0.8"));
        LoadRequest request = createLoadRequest(
            "1234",
            "1234",
            formatAmount(amount),
            "2018-01-01T00:00:00Z"
        );

        LoadResponse response = loadLimitService.processLoad(request);

        assertTrue(response.isAccepted());
        assertEquals("1234", response.getId());
        assertEquals("1234", response.getCustomerId());
    }

    @Test
    void shouldRejectDailyLimitExceeded() {
        // First load at 90% of daily limit
        BigDecimal firstAmount = dailyLoadLimit.multiply(new BigDecimal("0.9"));
        LoadRequest request1 = createLoadRequest(
            "1",
            "1234",
            formatAmount(firstAmount),
            "2018-01-01T00:00:00Z"
        );

        // Second load that would exceed daily limit (20% of daily limit)
        BigDecimal secondAmount = dailyLoadLimit.multiply(new BigDecimal("0.2"));
        LoadRequest request2 = createLoadRequest(
            "2",
            "1234",
            formatAmount(secondAmount),
            "2018-01-01T01:00:00Z"
        );

        loadLimitService.processLoad(request1);
        LoadResponse response = loadLimitService.processLoad(request2);

        assertFalse(response.isAccepted());
    }

    @Test
    void shouldRejectDailyLoadCountExceeded() {
        // Use 1% of daily limit to ensure we're testing count limit, not amount limit
        BigDecimal smallAmount = dailyLoadLimit.multiply(new BigDecimal("0.01"));
        
        // Make maxDailyLoads number of loads
        for (int i = 1; i <= maxDailyLoads; i++) {
            loadLimitService.processLoad(createLoadRequest(
                String.valueOf(i),
                "1234",
                formatAmount(smallAmount),
                "2025-02-10T00:00:00Z"
            ));
        }

        // One more load should be rejected
        LoadResponse response = loadLimitService.processLoad(createLoadRequest(
            String.valueOf(maxDailyLoads + 1),
            "1234",
            formatAmount(smallAmount),
            "2025-02-10T00:00:00Z"
        ));

        assertFalse(response.isAccepted());
    }

    @Test
    void shouldAcceptLoadAfterDailyReset() {
        // Use small amount to avoid hitting amount limits
        BigDecimal amount = dailyLoadLimit.multiply(new BigDecimal("0.1"));
        
        // Process 3 loads (daily limit) for first day
        for (int i = 0; i < 3; i++) {
            LoadResponse response = loadLimitService.processLoad(createLoadRequest(
                String.valueOf(i + 1),
                "1234",
                formatAmount(amount),
                "2025-02-10T00:00:00Z"
            ));
            assertTrue(response.isAccepted());
        }

        // Load on next day should be accepted
        LoadResponse response = loadLimitService.processLoad(createLoadRequest(
            "4",
            "1234",
            formatAmount(amount),
            "2025-02-11T00:00:00Z"
        ));

        assertTrue(response.isAccepted());
    }

    @Test
    void shouldRejectWeeklyLimitExceeded() {
        // Use 20% of weekly limit for each load
        BigDecimal loadAmount = weeklyLoadLimit.multiply(new BigDecimal("0.2"));
        
        // Load 5 times throughout the week (total 100% of weekly limit)
        for (int i = 0; i < 5; i++) {
            loadLimitService.processLoad(createLoadRequest(
                String.valueOf(i + 1),
                "1234",
                formatAmount(loadAmount),
                "2025-02-" + (10 + i) + "T00:00:00Z"
            ));
        }

        // This load should exceed weekly limit
        LoadResponse response = loadLimitService.processLoad(createLoadRequest(
            "6",
            "1234",
            formatAmount(loadAmount),
            "2025-02-15T00:00:00Z"
        ));

        assertFalse(response.isAccepted());
    }

    @Test
    void shouldAcceptLoadAtWeeklyLimit() {
        // Use 25% of weekly limit for each load
        BigDecimal loadAmount = weeklyLoadLimit.multiply(new BigDecimal("0.25"));
        
        // Load 4 times throughout the week (total 100% of weekly limit)
        for (int i = 0; i < 4; i++) {
            LoadResponse response = loadLimitService.processLoad(createLoadRequest(
                String.valueOf(i + 1),
                "1234",
                formatAmount(loadAmount),
                "2025-02-" + (10 + i) + "T00:00:00Z"
            ));
            assertTrue(response.isAccepted(), "Load " + (i + 1) + " should be accepted");
        }
    }

    @Test
    void shouldAcceptLoadAfterWeeklyReset() {
        // Use small amount to avoid hitting amount limits
        BigDecimal amount = weeklyLoadLimit.multiply(new BigDecimal("0.25"));
        
        // Process 4 loads (weekly limit) for first week
        for (int i = 0; i < 4; i++) {
            LoadResponse response = loadLimitService.processLoad(createLoadRequest(
                String.valueOf(i + 1),
                "1234",
                formatAmount(amount),
                "2025-02-" + (10 + i) + "T00:00:00Z"
            ));
            assertTrue(response.isAccepted());
        }

        // Load on next week should be accepted
        LoadResponse response = loadLimitService.processLoad(createLoadRequest(
            "7",
            "1234",
            formatAmount(amount),
            "2025-02-20T00:00:00Z"
        ));

        assertTrue(response.isAccepted());
    }

    @Test
    void shouldIgnoreLoadsWithSameId() {
        // Use 50% of daily limit
        BigDecimal amount = dailyLoadLimit.multiply(new BigDecimal("0.5"));
        
        LoadResponse response = loadLimitService.processLoad(createLoadRequest(
            "1", "1234", formatAmount(amount), "2025-02-10T00:00:00Z"
        ));
        

        // Attempt same load ID with different amount
        LoadResponse response2 = loadLimitService.processLoad(createLoadRequest(
            "1", "1234", formatAmount(amount), "2025-02-14T00:00:00Z"
        ));

        assertNotNull(response);
        assertNull(response2);
    }
}

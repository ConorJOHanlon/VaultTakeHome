package com.velocity.limits.repository;

import com.velocity.limits.entity.CustomerLoad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public interface CustomerLoadRepository extends JpaRepository<CustomerLoad, Long> {
    boolean existsByLoadIdAndCustomerId(String loadId, String customerId);
    
    @Query("SELECT SUM(c.amount) FROM CustomerLoad c WHERE c.customerId = ?1 AND c.loadTime >= ?2 AND c.loadTime <= ?3")
    BigDecimal sumAmountByCustomerIdAndLoadTimeBetween(String customerId, ZonedDateTime start, ZonedDateTime end);
    
    @Query("SELECT COUNT(c) FROM CustomerLoad c WHERE c.customerId = ?1 AND c.loadTime >= ?2 AND c.loadTime <= ?3")
    long countByCustomerIdAndLoadTimeBetween(String customerId, ZonedDateTime start, ZonedDateTime end);
} 
package com.velocity.limits.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Data
public class CustomerLoad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String loadId;
    private String customerId;
    private BigDecimal amount;
    private ZonedDateTime loadTime;
} 
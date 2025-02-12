package com.velocity.limits.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
public class LoadRequest {
    private String id;
    
    @JsonProperty("customer_id")
    private String customerId;
    
    @JsonProperty("load_amount")
    private String loadAmount;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private ZonedDateTime time;
    
    public BigDecimal getLoadAmountValue() {
        return new BigDecimal(loadAmount.replace("$", ""));
    }
} 
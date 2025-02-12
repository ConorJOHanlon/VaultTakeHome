package com.velocity.limits.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonPropertyOrder({"id", "customer_id", "accepted"})
public class LoadResponse {
    private String id;
    
    @JsonProperty("customer_id")
    private String customerId;
    
    private boolean accepted;
} 
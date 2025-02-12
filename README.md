# Vault Take Home

## Overview

The Velocity Limits Service provides functionality for:
- Configuring and managing daily and weekly transaction velocity limits

## Usage
- The service will read the input file and process the load requests e.g:
```
{
    "id": "1234",
    "customer_id": "1234",
    "load_amount": "$100.00",
    "time": "2025-02-10T00:00:00Z"
}
```
- The service will write the output to the output.txt file in the following format:
```
{
    "id": "1234",
    "customer_id": "1234",
    "accepted": true // true if the load request is accepted, false otherwise
}
```

## Limits
Limits are configured in the `application.properties` file.

## Setup
1. Clone the repository
2. Run `mvn clean install`
3. Run `mvn spring-boot:run`

## Metrics
- The service will expose metrics for the following:
    - Total number of load attempts
    - Total number of accepted loads
    - Total number of rejected loads
    - Total number of duplicate loads
    - Total number of daily limit exceeded
    - Total number of weekly limit exceeded


## Alerts
- The service will send alerts for the following:
    - High rejection rate
    - High load volume

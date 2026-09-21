package com.bankingsim.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(
        Long accNo,
        String type,
        BigDecimal balance,
        String status,
        BigDecimal minimumBalance,
        BigDecimal overdraftLimit,
        String username,
        LocalDateTime createdAt
) {}

package com.bankingsim.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminUserSummaryResponse(
        Long userId,
        String username,
        String role,
        String status,
        int accountCount,
        BigDecimal totalBalance,
        LocalDateTime createdAt
) {}

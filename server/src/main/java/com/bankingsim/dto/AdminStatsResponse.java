package com.bankingsim.dto;

import java.math.BigDecimal;

public record AdminStatsResponse(
        long totalUsers,
        long totalAccounts,
        long activeAccounts,
        long frozenAccounts,
        long closedAccounts,
        BigDecimal totalSystemBalance,
        long totalTransactions
) {}

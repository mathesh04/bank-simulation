package com.bankingsim.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminActivityResponse(
        Long txnId,
        Long accNo,
        String username,
        String accountType,
        String accountStatus,
        BigDecimal amount,
        String txnType,
        LocalDateTime txnDate
) {}

package com.bankingsim.dto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record TransactionResponse(Long txnId, Long accNo, BigDecimal amount, String txnType, LocalDateTime txnDate) {}

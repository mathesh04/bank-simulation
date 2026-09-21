package com.bankingsim.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TransferRequest(
        @NotNull Long fromAccNo,
        Long toAccNo,
        String recipientUsername,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount
) {}

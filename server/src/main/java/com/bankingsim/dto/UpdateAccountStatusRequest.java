package com.bankingsim.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateAccountStatusRequest(
        @NotBlank String status
) {}

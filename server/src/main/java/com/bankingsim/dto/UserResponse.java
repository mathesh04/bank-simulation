package com.bankingsim.dto;

import java.time.LocalDateTime;

public record UserResponse(
        Long userId,
        String username,
        String role,
        String status,
        LocalDateTime createdAt
) {}

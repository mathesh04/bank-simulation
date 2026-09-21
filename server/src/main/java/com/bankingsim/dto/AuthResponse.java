package com.bankingsim.dto;

public record AuthResponse(
        String token,
        Long userId,
        String username,
        String role,
        String status
) {}

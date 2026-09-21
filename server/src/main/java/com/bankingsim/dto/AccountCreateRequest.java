package com.bankingsim.dto;
import jakarta.validation.constraints.NotBlank;
public record AccountCreateRequest(@NotBlank String type) {}

package com.bankingsim.dto;

import java.util.List;

public record RecipientResponse(
        Long userId,
        String username,
        List<RecipientAccountSummary> accounts
) {
    public record RecipientAccountSummary(
            Long accNo,
            String type,
            String status
    ) {}
}

package com.bankingsim.controller;

import com.bankingsim.dto.TransactionResponse;
import com.bankingsim.model.Transaction;
import com.bankingsim.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final AccountService accountService;

    @GetMapping("/{accNo}")
    public ResponseEntity<List<TransactionResponse>> getTransactionHistory(
            @PathVariable Long accNo, Authentication auth) {
        List<Transaction> transactions = accountService.getTransactionHistory(accNo, auth.getName());
        return ResponseEntity.ok(transactions.stream()
                .map(t -> new TransactionResponse(t.getTxnId(), t.getAccount().getAccNo(),
                        t.getAmount(), t.getTxnType(), t.getTxnDate()))
                .toList());
    }
}

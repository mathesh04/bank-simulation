package com.bankingsim.controller;

import com.bankingsim.dto.*;
import com.bankingsim.model.Account;
import com.bankingsim.service.AccountService;
import com.bankingsim.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getMyAccounts(Authentication auth) {
        return ResponseEntity.ok(accountService.getAccountsByUsername(auth.getName()).stream()
                .map(this::toResponse).toList());
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody AccountCreateRequest request, Authentication auth) {
        Account account = accountService.createAccount(userService.findByUsername(auth.getName()), request.type());
        return ResponseEntity.ok(toResponse(account));
    }

    @GetMapping("/{accNo}/balance")
    public ResponseEntity<?> getBalance(@PathVariable Long accNo, Authentication auth) {
        return ResponseEntity.ok(Map.of("accNo", accNo, "balance", accountService.getBalance(accNo, auth.getName())));
    }

    @PostMapping("/{accNo}/deposit")
    public ResponseEntity<?> deposit(@PathVariable Long accNo, @Valid @RequestBody AmountRequest request, Authentication auth) {
        accountService.deposit(accNo, request.amount(), auth.getName());
        return ResponseEntity.ok(Map.of("message", "Deposit of ₹" + request.amount() + " successful"));
    }

    @PostMapping("/{accNo}/withdraw")
    public ResponseEntity<?> withdraw(@PathVariable Long accNo, @Valid @RequestBody AmountRequest request, Authentication auth) {
        accountService.withdraw(accNo, request.amount(), auth.getName());
        return ResponseEntity.ok(Map.of("message", "Withdrawal of ₹" + request.amount() + " successful"));
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@Valid @RequestBody TransferRequest request, Authentication auth) {
        accountService.transfer(request.fromAccNo(), request.toAccNo(), request.recipientUsername(), request.amount(), auth.getName());
        return ResponseEntity.ok(Map.of("message", "Transfer of ₹" + request.amount() + " successful"));
    }

    @PostMapping("/{accNo}/close")
    public ResponseEntity<?> closeAccount(@PathVariable Long accNo, Authentication auth) {
        accountService.closeAccount(accNo, auth.getName());
        return ResponseEntity.ok(Map.of("message", "Account #" + accNo + " closed successfully"));
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getAccNo(),
                account.getType(),
                account.getBalance(),
                account.getStatus() != null ? account.getStatus().name() : "ACTIVE",
                account.getMinimumBalance(),
                account.getOverdraftLimit(),
                account.getUser() == null ? null : account.getUser().getUsername(),
                account.getCreatedAt()
        );
    }
}

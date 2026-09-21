package com.bankingsim.model;

import com.bankingsim.exception.AccountClosedException;
import com.bankingsim.exception.AccountFrozenException;
import com.bankingsim.exception.InsufficientFundsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    private Account savingsAccount;
    private Account checkingAccount;

    @BeforeEach
    void setUp() {
        savingsAccount = new Account();
        savingsAccount.setAccNo(101L);
        savingsAccount.setType("SAVINGS");
        savingsAccount.setStatus(AccountStatus.ACTIVE);
        savingsAccount.setBalance(new BigDecimal("1000.00"));
        savingsAccount.setMinimumBalance(new BigDecimal("500.00"));
        savingsAccount.setOverdraftLimit(BigDecimal.ZERO);

        checkingAccount = new Account();
        checkingAccount.setAccNo(102L);
        checkingAccount.setType("CHECKING");
        checkingAccount.setStatus(AccountStatus.ACTIVE);
        checkingAccount.setBalance(new BigDecimal("100.00"));
        checkingAccount.setMinimumBalance(BigDecimal.ZERO);
        checkingAccount.setOverdraftLimit(new BigDecimal("5000.00"));
    }

    @Test
    @DisplayName("Deposit increases balance on active account")
    void testDepositActive() {
        savingsAccount.deposit(new BigDecimal("250.00"));
        assertEquals(new BigDecimal("1250.00"), savingsAccount.getBalance());
    }

    @Test
    @DisplayName("Deposit is permitted on FROZEN account (incoming funds allowed)")
    void testDepositFrozenAccountPermitted() {
        savingsAccount.freeze();
        assertEquals(AccountStatus.FROZEN, savingsAccount.getStatus());
        savingsAccount.deposit(new BigDecimal("300.00"));
        assertEquals(new BigDecimal("1300.00"), savingsAccount.getBalance());
    }

    @Test
    @DisplayName("Deposit is rejected on CLOSED account")
    void testDepositClosedAccountFails() {
        savingsAccount.setBalance(BigDecimal.ZERO);
        savingsAccount.close();
        assertEquals(AccountStatus.CLOSED, savingsAccount.getStatus());

        assertThrows(AccountClosedException.class, () -> {
            savingsAccount.deposit(new BigDecimal("100.00"));
        });
    }

    @Test
    @DisplayName("Withdraw is rejected on FROZEN account")
    void testWithdrawFrozenAccountFails() {
        savingsAccount.freeze();
        assertThrows(AccountFrozenException.class, () -> {
            savingsAccount.withdraw(new BigDecimal("100.00"));
        });
    }

    @Test
    @DisplayName("Savings Account enforces minimum balance policy of ₹500")
    void testSavingsMinimumBalanceEnforced() {
  
        savingsAccount.withdraw(new BigDecimal("400.00"));
        assertEquals(new BigDecimal("600.00"), savingsAccount.getBalance());


        assertThrows(InsufficientFundsException.class, () -> {
            savingsAccount.withdraw(new BigDecimal("200.00"));
        });
    }

    @Test
    @DisplayName("Checking Account allows overdraft up to ₹5000 limit")
    void testCheckingOverdraftAllowed() {

        checkingAccount.withdraw(new BigDecimal("500.00"));
        assertEquals(new BigDecimal("-400.00"), checkingAccount.getBalance());

        checkingAccount.withdraw(new BigDecimal("4600.00"));
        assertEquals(new BigDecimal("-5000.00"), checkingAccount.getBalance());

        assertThrows(InsufficientFundsException.class, () -> {
            checkingAccount.withdraw(new BigDecimal("1.00"));
        });
    }

    @Test
    @DisplayName("Cannot close account with remaining balance")
    void testCloseAccountWithBalanceFails() {
        assertThrows(IllegalStateException.class, () -> {
            savingsAccount.close();
        });
    }
}

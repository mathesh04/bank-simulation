package com.bankingsim.model;

import com.bankingsim.exception.AccountClosedException;
import com.bankingsim.exception.AccountFrozenException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Account {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long accNo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String type = "SAVINGS";

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal minimumBalance = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal overdraftLimit = BigDecimal.ZERO;

    @Column
    private LocalDateTime createdAt = LocalDateTime.now();

    @JsonIgnore
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactions = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (status == null) status = AccountStatus.ACTIVE;
        if (balance == null) balance = BigDecimal.ZERO;
        if (createdAt == null) createdAt = LocalDateTime.now();
        AccountType policy = getAccountTypePolicy();
        if (minimumBalance == null || minimumBalance.compareTo(BigDecimal.ZERO) == 0) {
            this.minimumBalance = policy.getMinimumBalance();
        }
        if (overdraftLimit == null || overdraftLimit.compareTo(BigDecimal.ZERO) == 0) {
            this.overdraftLimit = policy.getOverdraftLimit();
        }
    }

    public AccountStatus getStatus() {
        return status != null ? status : AccountStatus.ACTIVE;
    }

    public BigDecimal getMinimumBalance() {
        if (minimumBalance != null) return minimumBalance;
        return getAccountTypePolicy().getMinimumBalance();
    }

    public BigDecimal getOverdraftLimit() {
        if (overdraftLimit != null) return overdraftLimit;
        return getAccountTypePolicy().getOverdraftLimit();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt != null ? createdAt : LocalDateTime.now();
    }

    public AccountType getAccountTypePolicy() {
        return AccountType.fromString(this.type);
    }


    public void deposit(BigDecimal amount) {
        validatePositive(amount);
        if (getStatus() == AccountStatus.CLOSED) {
            throw new AccountClosedException("Account #" + this.accNo + " is closed. Cannot deposit.");
        }
        this.balance = (this.balance != null ? this.balance : BigDecimal.ZERO).add(amount);
    }


    public void withdraw(BigDecimal amount) {
        validatePositive(amount);
        if (getStatus() == AccountStatus.CLOSED) {
            throw new AccountClosedException("Account #" + this.accNo + " is closed. Cannot withdraw.");
        }
        if (getStatus() == AccountStatus.FROZEN) {
            throw new AccountFrozenException("Account #" + this.accNo + " is frozen. Withdrawals and outbound transfers are prohibited.");
        }

        BigDecimal curBalance = this.balance != null ? this.balance : BigDecimal.ZERO;

        getAccountTypePolicy().validateWithdrawal(curBalance, amount);

        this.balance = curBalance.subtract(amount);
    }

    public void freeze() {
        if (getStatus() == AccountStatus.CLOSED) {
            throw new AccountClosedException("Cannot freeze a closed account.");
        }
        this.status = AccountStatus.FROZEN;
    }

    public void unfreeze() {
        if (getStatus() == AccountStatus.CLOSED) {
            throw new AccountClosedException("Cannot unfreeze a closed account.");
        }
        this.status = AccountStatus.ACTIVE;
    }

    public void close() {
        BigDecimal curBalance = this.balance != null ? this.balance : BigDecimal.ZERO;
        if (curBalance.compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("Cannot close account #" + this.accNo + " with non-zero balance: ₹" + curBalance);
        }
        this.status = AccountStatus.CLOSED;
    }

    private void validatePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Transaction amount must be greater than ₹0.00");
        }
    }
}

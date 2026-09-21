package com.bankingsim.model;

import com.bankingsim.exception.InsufficientFundsException;
import java.math.BigDecimal;

public enum AccountType {
    SAVINGS {
        @Override
        public BigDecimal getMinimumBalance() {
            return new BigDecimal("500.00");
        }

        @Override
        public BigDecimal getOverdraftLimit() {
            return BigDecimal.ZERO;
        }

        @Override
        public void validateWithdrawal(BigDecimal currentBalance, BigDecimal amount) {
            BigDecimal minimumBalance = getMinimumBalance();
            BigDecimal remaining = currentBalance.subtract(amount);
            if (remaining.compareTo(minimumBalance) < 0) {
                throw new InsufficientFundsException(
                        "Savings accounts require a minimum balance of ₹" + minimumBalance
                        + ". Current balance: ₹" + currentBalance);
            }
        }
    },
    CHECKING {
        @Override
        public BigDecimal getMinimumBalance() {
            return BigDecimal.ZERO;
        }

        @Override
        public BigDecimal getOverdraftLimit() {
            return new BigDecimal("5000.00");
        }

        @Override
        public void validateWithdrawal(BigDecimal currentBalance, BigDecimal amount) {
            BigDecimal overdraftLimit = getOverdraftLimit();
            BigDecimal remaining = currentBalance.subtract(amount);

            if (remaining.compareTo(overdraftLimit.negate()) < 0) {
                throw new InsufficientFundsException(
                        "Withdrawal exceeds checking overdraft limit of ₹" + overdraftLimit
                        + ". Maximum available: ₹" + currentBalance.add(overdraftLimit));
            }
        }
    };

    public abstract BigDecimal getMinimumBalance();
    public abstract BigDecimal getOverdraftLimit();
    public abstract void validateWithdrawal(BigDecimal currentBalance, BigDecimal amount);

    public static AccountType fromString(String type) {
        if (type == null) return SAVINGS;
        try {
            return AccountType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return SAVINGS;
        }
    }
}

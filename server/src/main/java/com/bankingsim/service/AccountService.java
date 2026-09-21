package com.bankingsim.service;

import com.bankingsim.exception.AccountNotFoundException;
import com.bankingsim.exception.UserNotFoundException;
import com.bankingsim.model.*;
import com.bankingsim.repository.AccountRepository;
import com.bankingsim.repository.TransactionRepository;
import com.bankingsim.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional
    public Account createAccount(User user, String rawType) {
        AccountType accountType = AccountType.fromString(rawType);
        Account account = new Account();
        account.setUser(user);
        account.setType(accountType.name());
        account.setStatus(AccountStatus.ACTIVE);
        account.setBalance(BigDecimal.ZERO);
        account.setMinimumBalance(accountType.getMinimumBalance());
        account.setOverdraftLimit(accountType.getOverdraftLimit());
        account.setCreatedAt(LocalDateTime.now());
        return accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public Account getAccount(Long accNo) {
        return accountRepository.findById(accNo)
                .orElseThrow(() -> new AccountNotFoundException("Account #" + accNo + " not found"));
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long accNo, String username) {
        return ownedAccount(accNo, username).getBalance();
    }

    @Transactional(readOnly = true)
    public List<Account> getAccountsByUsername(String username) {
        return accountRepository.findByUser_Username(username);
    }

    @Transactional
    public void deposit(Long accNo, BigDecimal amount, String username) {
        Account account = ownedAccount(accNo, username);
        account.deposit(amount);
        accountRepository.save(account);
        logTransaction(account, amount, "DEPOSIT");
    }

    @Transactional
    public void withdraw(Long accNo, BigDecimal amount, String username) {
        Account account = ownedAccount(accNo, username);
        account.withdraw(amount);
        accountRepository.save(account);
        logTransaction(account, amount, "WITHDRAW");
    }

    @Transactional
    public void transfer(Long fromAccNo, Long toAccNo, String recipientUsername, BigDecimal amount, String currentUsername) {
        Account fromAccount = ownedAccount(fromAccNo, currentUsername);

        Account toAccount;
        if (toAccNo != null) {
            toAccount = getAccount(toAccNo);
            if (recipientUsername != null && !recipientUsername.trim().isEmpty()) {
                if (toAccount.getUser() == null || !recipientUsername.trim().equalsIgnoreCase(toAccount.getUser().getUsername())) {
                    throw new IllegalArgumentException("Account #" + toAccNo + " does not belong to user '" + recipientUsername + "'");
                }
            }
        } else if (recipientUsername != null && !recipientUsername.trim().isEmpty()) {
            User recipient = userRepository.findByUsername(recipientUsername.trim())
                    .orElseThrow(() -> new UserNotFoundException("Recipient user '" + recipientUsername + "' not found"));
            if (!recipient.isActive()) {
                throw new IllegalStateException("Recipient user '" + recipientUsername + "' is suspended.");
            }
            List<Account> recipientAccounts = accountRepository.findByUser_UserId(recipient.getUserId()).stream()
                    .filter(a -> a.getStatus() != AccountStatus.CLOSED)
                    .toList();
            if (recipientAccounts.isEmpty()) {
                throw new IllegalStateException("Recipient user '" + recipientUsername + "' has no open accounts to receive funds.");
            }
            toAccount = recipientAccounts.get(0);
        } else {
            throw new IllegalArgumentException("Either destination account number or recipient username must be provided");
        }

        if (fromAccount.getAccNo().equals(toAccount.getAccNo())) {
            throw new IllegalArgumentException("Source and destination accounts must be different");
        }

        fromAccount.withdraw(amount);


        toAccount.deposit(amount);

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        String toUsername = toAccount.getUser() != null ? toAccount.getUser().getUsername() : "User";
        String fromUsername = fromAccount.getUser() != null ? fromAccount.getUser().getUsername() : currentUsername;

        logTransaction(fromAccount, amount, "TRANSFER_OUT (To " + toUsername + " #" + toAccount.getAccNo() + ")");
        logTransaction(toAccount, amount, "TRANSFER_IN (From " + fromUsername + " #" + fromAccNo + ")");
    }

    @Transactional
    public void transfer(Long fromAccNo, Long toAccNo, BigDecimal amount, String currentUsername) {
        transfer(fromAccNo, toAccNo, null, amount, currentUsername);
    }

    @Transactional
    public void closeAccount(Long accNo, String username) {
        Account account = ownedAccount(accNo, username);
        account.close();
        accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionHistory(Long accNo, String username) {
        ownedAccount(accNo, username);
        return transactionRepository.findByAccount_AccNoOrderByTxnDateDesc(accNo);
    }

    @Transactional(readOnly = true)
    public Account ownedAccount(Long accNo, String username) {
        Account account = getAccount(accNo);
        if (account.getUser() == null || !username.equals(account.getUser().getUsername())) {
            throw new AccountNotFoundException("Account #" + accNo + " does not belong to user '" + username + "'");
        }
        return account;
    }

    private void logTransaction(Account account, BigDecimal amount, String type) {
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setAmount(amount);
        transaction.setTxnType(type);
        transaction.setTxnDate(LocalDateTime.now());
        transactionRepository.save(transaction);
    }
}

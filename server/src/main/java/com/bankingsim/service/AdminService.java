package com.bankingsim.service;

import com.bankingsim.dto.*;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        long totalUsers = userRepository.count();
        long totalAccounts = accountRepository.count();
        long activeAccounts = accountRepository.countByStatus(AccountStatus.ACTIVE);
        long frozenAccounts = accountRepository.countByStatus(AccountStatus.FROZEN);
        long closedAccounts = accountRepository.countByStatus(AccountStatus.CLOSED);
        BigDecimal totalLiquidity = accountRepository.sumTotalActiveBalance();
        long totalTransactions = transactionRepository.count();

        return new AdminStatsResponse(
                totalUsers,
                totalAccounts,
                activeAccounts,
                frozenAccounts,
                closedAccounts,
                totalLiquidity != null ? totalLiquidity : BigDecimal.ZERO,
                totalTransactions
        );
    }

    @Transactional(readOnly = true)
    public List<AdminUserSummaryResponse> getAllUsers() {
        return userRepository.findAllByOrderByUserIdAsc().stream().map(u -> {
            List<Account> accounts = accountRepository.findByUser_UserId(u.getUserId());
            BigDecimal totalBalance = accounts.stream()
                    .map(Account::getBalance)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return new AdminUserSummaryResponse(
                    u.getUserId(),
                    u.getUsername(),
                    u.getRole() != null ? u.getRole().name() : Role.ROLE_CUSTOMER.name(),
                    u.getStatus() != null ? u.getStatus().name() : UserStatus.ACTIVE.name(),
                    accounts.size(),
                    totalBalance,
                    u.getCreatedAt()
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User #" + userId + " not found"));
    }

    @Transactional
    public User updateUserStatus(Long userId, String statusStr) {
        User user = getUser(userId);
        UserStatus status = UserStatus.valueOf(statusStr.trim().toUpperCase());
        if (status == UserStatus.SUSPENDED) {
            user.suspend();
        } else {
            user.activate();
        }
        return userRepository.save(user);
    }

    @Transactional
    public User updateUserRole(Long userId, String roleStr) {
        User user = getUser(userId);
        String formatted = roleStr.trim().toUpperCase();
        if (!formatted.startsWith("ROLE_")) {
            formatted = "ROLE_" + formatted;
        }
        Role role = Role.valueOf(formatted);
        user.setRole(role);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findAllByOrderByAccNoAsc().stream().map(a -> new AccountResponse(
                a.getAccNo(),
                a.getType(),
                a.getBalance(),
                a.getStatus() != null ? a.getStatus().name() : AccountStatus.ACTIVE.name(),
                a.getMinimumBalance(),
                a.getOverdraftLimit(),
                a.getUser() != null ? a.getUser().getUsername() : null,
                a.getCreatedAt()
        )).toList();
    }

    @Transactional
    public Account updateAccountStatus(Long accNo, String statusStr) {
        Account account = accountRepository.findById(accNo)
                .orElseThrow(() -> new AccountNotFoundException("Account #" + accNo + " not found"));
        AccountStatus targetStatus = AccountStatus.valueOf(statusStr.trim().toUpperCase());

        if (targetStatus == AccountStatus.FROZEN) {
            account.freeze();
        } else if (targetStatus == AccountStatus.ACTIVE) {
            account.unfreeze();
        } else if (targetStatus == AccountStatus.CLOSED) {
            account.close();
        }
        return accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public List<AdminActivityResponse> getAllActivities() {
        return transactionRepository.findAllByOrderByTxnDateDesc().stream().map(t -> {
            Account acc = t.getAccount();
            String username = (acc != null && acc.getUser() != null) ? acc.getUser().getUsername() : "Unknown";
            String accType = acc != null ? acc.getType() : "N/A";
            String accStatus = (acc != null && acc.getStatus() != null) ? acc.getStatus().name() : "N/A";
            Long accNo = acc != null ? acc.getAccNo() : null;

            return new AdminActivityResponse(
                    t.getTxnId(),
                    accNo,
                    username,
                    accType,
                    accStatus,
                    t.getAmount(),
                    t.getTxnType(),
                    t.getTxnDate()
            );
        }).toList();
    }
}

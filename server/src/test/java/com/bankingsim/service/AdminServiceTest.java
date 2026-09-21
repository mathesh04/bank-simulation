package com.bankingsim.service;

import com.bankingsim.dto.AdminStatsResponse;
import com.bankingsim.dto.AdminUserSummaryResponse;
import com.bankingsim.model.*;
import com.bankingsim.repository.AccountRepository;
import com.bankingsim.repository.TransactionRepository;
import com.bankingsim.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AdminService adminService;

    private User sampleUser;
    private Account sampleAccount;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setUserId(1L);
        sampleUser.setUsername("testuser");
        sampleUser.setRole(Role.ROLE_CUSTOMER);
        sampleUser.setStatus(UserStatus.ACTIVE);
        sampleUser.setCreatedAt(LocalDateTime.now());

        sampleAccount = new Account();
        sampleAccount.setAccNo(501L);
        sampleAccount.setUser(sampleUser);
        sampleAccount.setType("SAVINGS");
        sampleAccount.setStatus(AccountStatus.ACTIVE);
        sampleAccount.setBalance(new BigDecimal("1500.00"));
        sampleAccount.setMinimumBalance(new BigDecimal("500.00"));
        sampleAccount.setOverdraftLimit(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Admin getStats aggregates total users, accounts, liquidity, and transactions")
    void testGetStats() {
        when(userRepository.count()).thenReturn(10L);
        when(accountRepository.count()).thenReturn(20L);
        when(accountRepository.countByStatus(AccountStatus.ACTIVE)).thenReturn(18L);
        when(accountRepository.countByStatus(AccountStatus.FROZEN)).thenReturn(2L);
        when(accountRepository.countByStatus(AccountStatus.CLOSED)).thenReturn(0L);
        when(accountRepository.sumTotalActiveBalance()).thenReturn(new BigDecimal("75000.00"));
        when(transactionRepository.count()).thenReturn(150L);

        AdminStatsResponse stats = adminService.getStats();

        assertEquals(10L, stats.totalUsers());
        assertEquals(20L, stats.totalAccounts());
        assertEquals(18L, stats.activeAccounts());
        assertEquals(2L, stats.frozenAccounts());
        assertEquals(new BigDecimal("75000.00"), stats.totalSystemBalance());
        assertEquals(150L, stats.totalTransactions());
    }

    @Test
    @DisplayName("Admin can suspend an active user")
    void testSuspendUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User updated = adminService.updateUserStatus(1L, "SUSPENDED");

        assertEquals(UserStatus.SUSPENDED, updated.getStatus());
        assertFalse(updated.isActive());
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("Admin can promote a customer to administrator")
    void testPromoteUserRole() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User updated = adminService.updateUserRole(1L, "ROLE_ADMIN");

        assertEquals(Role.ROLE_ADMIN, updated.getRole());
        assertTrue(updated.isAdmin());
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("Admin can freeze and unfreeze an account")
    void testFreezeAndUnfreezeAccount() {
        when(accountRepository.findById(501L)).thenReturn(Optional.of(sampleAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));

        Account frozen = adminService.updateAccountStatus(501L, "FROZEN");
        assertEquals(AccountStatus.FROZEN, frozen.getStatus());

        Account unfrozen = adminService.updateAccountStatus(501L, "ACTIVE");
        assertEquals(AccountStatus.ACTIVE, unfrozen.getStatus());
    }

    @Test
    @DisplayName("Admin getAllUsers calculates total balance across accounts for each user")
    void testGetAllUsersCalculatesBalances() {
        when(userRepository.findAllByOrderByUserIdAsc()).thenReturn(List.of(sampleUser));
        when(accountRepository.findByUser_UserId(1L)).thenReturn(List.of(sampleAccount));

        List<AdminUserSummaryResponse> users = adminService.getAllUsers();

        assertEquals(1, users.size());
        assertEquals("testuser", users.get(0).username());
        assertEquals(1, users.get(0).accountCount());
        assertEquals(new BigDecimal("1500.00"), users.get(0).totalBalance());
    }
}

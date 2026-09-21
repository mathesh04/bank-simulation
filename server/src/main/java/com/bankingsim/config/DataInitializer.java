package com.bankingsim.config;

import com.bankingsim.model.Account;
import com.bankingsim.model.AccountStatus;
import com.bankingsim.model.Role;
import com.bankingsim.model.User;
import com.bankingsim.model.UserStatus;
import com.bankingsim.repository.AccountRepository;
import com.bankingsim.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking data initialization and auto-seeding admin user...");


        try {
            jdbcTemplate.execute("ALTER TABLE transactions ALTER COLUMN txn_type TYPE VARCHAR(255)");
            jdbcTemplate.execute("UPDATE users SET role = 'ROLE_CUSTOMER' WHERE role IS NULL");
            jdbcTemplate.execute("UPDATE users SET status = 'ACTIVE' WHERE status IS NULL");
            jdbcTemplate.execute("UPDATE users SET created_at = NOW() WHERE created_at IS NULL");
            jdbcTemplate.execute("UPDATE accounts SET status = 'ACTIVE' WHERE status IS NULL");
            jdbcTemplate.execute("UPDATE accounts SET created_at = NOW() WHERE created_at IS NULL");
            jdbcTemplate.execute("UPDATE accounts SET minimum_balance = CASE WHEN UPPER(type) = 'SAVINGS' THEN 500.00 ELSE 0.00 END WHERE minimum_balance IS NULL");
            jdbcTemplate.execute("UPDATE accounts SET overdraft_limit = CASE WHEN UPPER(type) = 'CHECKING' THEN 5000.00 ELSE 0.00 END WHERE overdraft_limit IS NULL");
        } catch (Exception ex) {
            log.warn("Notice: Column migration/backfill executed with notice: {}", ex.getMessage());
        }


        try {
            userRepository.findByUsername("admin").ifPresentOrElse(
                admin -> {
                    if (admin.getRole() != Role.ROLE_ADMIN || admin.getStatus() != UserStatus.ACTIVE) {
                        admin.setRole(Role.ROLE_ADMIN);
                        admin.setStatus(UserStatus.ACTIVE);
                        userRepository.save(admin);
                        log.info("Updated existing user 'admin' to ROLE_ADMIN and ACTIVE");
                    }
                },
                () -> {
                    User admin = new User();
                    admin.setUsername("admin");
                    admin.setPasswordHash(passwordEncoder.encode("Admin@123456"));
                    admin.setRole(Role.ROLE_ADMIN);
                    admin.setStatus(UserStatus.ACTIVE);
                    admin.setCreatedAt(LocalDateTime.now());
                    User savedAdmin = userRepository.save(admin);

                    Account defaultAccount = new Account();
                    defaultAccount.setUser(savedAdmin);
                    defaultAccount.setType("CHECKING");
                    defaultAccount.setStatus(AccountStatus.ACTIVE);
                    defaultAccount.setBalance(new BigDecimal("10000.00"));
                    defaultAccount.setMinimumBalance(BigDecimal.ZERO);
                    defaultAccount.setOverdraftLimit(new BigDecimal("5000.00"));
                    defaultAccount.setCreatedAt(LocalDateTime.now());
                    accountRepository.save(defaultAccount);

                    log.info("Auto-seeded default administrator account: username='admin', password='Admin@123456'");
                }
            );
        } catch (Exception ex) {
            log.error("Error ensuring admin account exists: {}", ex.getMessage(), ex);
        }
    }
}

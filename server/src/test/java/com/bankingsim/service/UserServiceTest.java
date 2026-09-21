package com.bankingsim.service;

import com.bankingsim.dto.RecipientResponse;
import com.bankingsim.exception.UserSuspendedException;
import com.bankingsim.model.Account;
import com.bankingsim.model.AccountStatus;
import com.bankingsim.model.Role;
import com.bankingsim.model.User;
import com.bankingsim.model.UserStatus;
import com.bankingsim.repository.AccountRepository;
import com.bankingsim.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User activeUser;
    private User suspendedUser;

    @BeforeEach
    void setUp() {
        activeUser = new User();
        activeUser.setUserId(1L);
        activeUser.setUsername("active_customer");
        activeUser.setPasswordHash("hashed_secret");
        activeUser.setRole(Role.ROLE_CUSTOMER);
        activeUser.setStatus(UserStatus.ACTIVE);
        activeUser.setCreatedAt(LocalDateTime.now());

        suspendedUser = new User();
        suspendedUser.setUserId(2L);
        suspendedUser.setUsername("suspended_customer");
        suspendedUser.setPasswordHash("hashed_secret");
        suspendedUser.setRole(Role.ROLE_CUSTOMER);
        suspendedUser.setStatus(UserStatus.SUSPENDED);
        suspendedUser.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Active user authenticates successfully with correct credentials")
    void testActiveUserAuthenticationSucceeds() {
        when(userRepository.findByUsername("active_customer")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("secret123", "hashed_secret")).thenReturn(true);

        User authenticated = userService.authenticate("active_customer", "secret123");
        assertNotNull(authenticated);
        assertEquals("active_customer", authenticated.getUsername());
    }

    @Test
    @DisplayName("Suspended user is blocked from authenticating even with correct password")
    void testSuspendedUserAuthenticationThrowsException() {
        when(userRepository.findByUsername("suspended_customer")).thenReturn(Optional.of(suspendedUser));
        when(passwordEncoder.matches("secret123", "hashed_secret")).thenReturn(true);

        assertThrows(UserSuspendedException.class, () -> {
            userService.authenticate("suspended_customer", "secret123");
        });
    }

    @Test
    @DisplayName("Invalid password throws BadCredentialsException")
    void testInvalidPasswordThrowsBadCredentials() {
        when(userRepository.findByUsername("active_customer")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrong_secret", "hashed_secret")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> {
            userService.authenticate("active_customer", "wrong_secret");
        });
    }

    @Test
    @DisplayName("Search recipients finds active users and excludes sender and closed accounts")
    void testSearchRecipients() {
        User recipient = new User();
        recipient.setUserId(3L);
        recipient.setUsername("recipient_user");
        recipient.setStatus(UserStatus.ACTIVE);

        Account openAccount = new Account();
        openAccount.setAccNo(201L);
        openAccount.setType("SAVINGS");
        openAccount.setStatus(AccountStatus.ACTIVE);

        Account closedAccount = new Account();
        closedAccount.setAccNo(202L);
        closedAccount.setType("CHECKING");
        closedAccount.setStatus(AccountStatus.CLOSED);

        when(userRepository.searchActiveRecipients("recip", "active_customer"))
                .thenReturn(List.of(recipient));
        when(accountRepository.findByUser_UserId(3L))
                .thenReturn(List.of(openAccount, closedAccount));

        List<RecipientResponse> results = userService.searchRecipients("recip", "active_customer");

        assertEquals(1, results.size());
        assertEquals("recipient_user", results.get(0).username());
        // Closed account must be excluded from recipient choices
        assertEquals(1, results.get(0).accounts().size());
        assertEquals(201L, results.get(0).accounts().get(0).accNo());
        assertEquals("SAVINGS", results.get(0).accounts().get(0).type());
    }
}

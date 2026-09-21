package com.bankingsim.service;

import com.bankingsim.dto.RecipientResponse;
import com.bankingsim.exception.UserNotFoundException;
import com.bankingsim.exception.UserSuspendedException;
import com.bankingsim.model.Account;
import com.bankingsim.model.AccountStatus;
import com.bankingsim.model.Role;
import com.bankingsim.model.User;
import com.bankingsim.model.UserStatus;
import com.bankingsim.repository.AccountRepository;
import com.bankingsim.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(String username, String password) {
        if (userRepository.findByUsername(username.trim()).isPresent()) {
            throw new DataIntegrityViolationException("Username already exists");
        }
        User user = new User();
        user.setUsername(username.trim());
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(Role.ROLE_CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    public User authenticate(String username, String rawPassword) {
        User user = findByUsername(username);
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }
        if (!user.isActive()) {
            throw new UserSuspendedException("Your user profile is SUSPENDED. Please contact bank administration.");
        }
        return user;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    public boolean checkPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    @Transactional(readOnly = true)
    public List<RecipientResponse> searchRecipients(String query, String currentUsername) {
        String cleanQuery = query == null ? "" : query.trim();
        List<User> users = userRepository.searchActiveRecipients(cleanQuery, currentUsername);
        return users.stream().map(u -> {
            List<Account> accounts = accountRepository.findByUser_UserId(u.getUserId()).stream()
                    .filter(a -> a.getStatus() != AccountStatus.CLOSED)
                    .toList();
            List<RecipientResponse.RecipientAccountSummary> accountSummaries = accounts.stream()
                    .map(a -> new RecipientResponse.RecipientAccountSummary(
                            a.getAccNo(),
                            a.getType(),
                            a.getStatus().name()
                    )).toList();
            return new RecipientResponse(u.getUserId(), u.getUsername(), accountSummaries);
        }).toList();
    }
}

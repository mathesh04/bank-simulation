package com.bankingsim.controller;

import com.bankingsim.dto.*;
import com.bankingsim.model.User;
import com.bankingsim.service.JwtService;
import com.bankingsim.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.registerUser(request.username(), request.password());
        return ResponseEntity.ok(toResponse(user));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.authenticate(request.username(), request.password());
        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
        return ResponseEntity.ok(new AuthResponse(
                token,
                user.getUserId(),
                user.getUsername(),
                user.getRole().name(),
                user.getStatus().name()
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        return ResponseEntity.ok(toResponse(userService.findByUsername(authentication.getName())));
    }

    @GetMapping("/recipients")
    public ResponseEntity<List<RecipientResponse>> searchRecipients(
            @RequestParam(defaultValue = "") String query, Authentication auth) {
        return ResponseEntity.ok(userService.searchRecipients(query, auth.getName()));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getUsername(),
                user.getRole() != null ? user.getRole().name() : "ROLE_CUSTOMER",
                user.getStatus() != null ? user.getStatus().name() : "ACTIVE",
                user.getCreatedAt()
        );
    }
}

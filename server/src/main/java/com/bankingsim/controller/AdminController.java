package com.bankingsim.controller;

import com.bankingsim.dto.*;
import com.bankingsim.model.Account;
import com.bankingsim.model.User;
import com.bankingsim.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserSummaryResponse>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long userId) {
        User u = adminService.getUser(userId);
        return ResponseEntity.ok(new UserResponse(
                u.getUserId(),
                u.getUsername(),
                u.getRole().name(),
                u.getStatus().name(),
                u.getCreatedAt()
        ));
    }

    @PutMapping("/users/{userId}/status")
    public ResponseEntity<?> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        User updated = adminService.updateUserStatus(userId, request.status());
        return ResponseEntity.ok(Map.of(
                "message", "User '" + updated.getUsername() + "' status updated to " + updated.getStatus(),
                "status", updated.getStatus().name()
        ));
    }

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<?> updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRoleRequest request) {
        User updated = adminService.updateUserRole(userId, request.role());
        return ResponseEntity.ok(Map.of(
                "message", "User '" + updated.getUsername() + "' role updated to " + updated.getRole(),
                "role", updated.getRole().name()
        ));
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        return ResponseEntity.ok(adminService.getAllAccounts());
    }

    @PutMapping("/accounts/{accNo}/status")
    public ResponseEntity<?> updateAccountStatus(
            @PathVariable Long accNo,
            @Valid @RequestBody UpdateAccountStatusRequest request) {
        Account updated = adminService.updateAccountStatus(accNo, request.status());
        return ResponseEntity.ok(Map.of(
                "message", "Account #" + accNo + " status changed to " + updated.getStatus(),
                "status", updated.getStatus().name()
        ));
    }

    @GetMapping("/activities")
    public ResponseEntity<List<AdminActivityResponse>> getAllActivities() {
        return ResponseEntity.ok(adminService.getAllActivities());
    }
}

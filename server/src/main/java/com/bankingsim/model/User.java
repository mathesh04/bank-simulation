package com.bankingsim.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(unique = true, nullable = false, length = 80)
    private String username;

    @JsonIgnore
    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Role role = Role.ROLE_CUSTOMER;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column
    private LocalDateTime createdAt = LocalDateTime.now();

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Account> accounts = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (role == null) role = Role.ROLE_CUSTOMER;
        if (status == null) status = UserStatus.ACTIVE;
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Role getRole() {
        return role != null ? role : Role.ROLE_CUSTOMER;
    }

    public UserStatus getStatus() {
        return status != null ? status : UserStatus.ACTIVE;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt != null ? createdAt : LocalDateTime.now();
    }

    public boolean isAdmin() {
        return getRole() == Role.ROLE_ADMIN;
    }

    public boolean isActive() {
        return getStatus() == UserStatus.ACTIVE;
    }

    public void suspend() {
        this.status = UserStatus.SUSPENDED;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public void promoteToAdmin() {
        this.role = Role.ROLE_ADMIN;
    }

    public void demoteToCustomer() {
        this.role = Role.ROLE_CUSTOMER;
    }
}

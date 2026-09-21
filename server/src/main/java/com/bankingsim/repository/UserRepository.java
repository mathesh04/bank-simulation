package com.bankingsim.repository;

import com.bankingsim.model.Role;
import com.bankingsim.model.User;
import com.bankingsim.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    long countByRole(Role role);
    long countByStatus(UserStatus status);
    List<User> findAllByOrderByUserIdAsc();

    @Query("SELECT u FROM User u WHERE (:query = '' OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))) AND u.status = com.bankingsim.model.UserStatus.ACTIVE AND u.username <> :currentUsername ORDER BY u.username ASC")
    List<User> searchActiveRecipients(@Param("query") String query, @Param("currentUsername") String currentUsername);
}

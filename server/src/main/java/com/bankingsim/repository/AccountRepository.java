package com.bankingsim.repository;

import com.bankingsim.model.Account;
import com.bankingsim.model.AccountStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    @EntityGraph(attributePaths = {"user"})
    List<Account> findByUser_UserId(Long userId);

    @EntityGraph(attributePaths = {"user"})
    List<Account> findByUser_Username(String username);

    @EntityGraph(attributePaths = {"user"})
    List<Account> findAllByOrderByAccNoAsc();

    long countByStatus(AccountStatus status);

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a WHERE a.status <> com.bankingsim.model.AccountStatus.CLOSED")
    BigDecimal sumTotalActiveBalance();
}

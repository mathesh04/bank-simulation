package com.bankingsim.repository;

import com.bankingsim.model.Transaction;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @EntityGraph(attributePaths = {"account", "account.user"})
    List<Transaction> findByAccount_AccNoOrderByTxnDateDesc(Long accNo);

    @EntityGraph(attributePaths = {"account", "account.user"})
    List<Transaction> findAllByOrderByTxnDateDesc();
}

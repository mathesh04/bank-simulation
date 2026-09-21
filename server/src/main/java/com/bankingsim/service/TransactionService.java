package com.bankingsim.service;

import com.bankingsim.model.Transaction;
import com.bankingsim.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public List<Transaction> getTransactionHistory(Long accNo) {
        return transactionRepository.findByAccount_AccNoOrderByTxnDateDesc(accNo);
    }
}

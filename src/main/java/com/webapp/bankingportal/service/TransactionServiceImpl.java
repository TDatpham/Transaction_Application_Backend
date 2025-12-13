package com.webapp.bankingportal.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.webapp.bankingportal.repository.AccountRepository;
import org.springframework.stereotype.Service;

import com.webapp.bankingportal.dto.TransactionDTO;
import com.webapp.bankingportal.entity.Account;
import com.webapp.bankingportal.entity.Transaction;
import com.webapp.bankingportal.mapper.TransactionMapper;
import com.webapp.bankingportal.repository.TransactionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TransactionServiceImpl implements TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionServiceImpl.class);

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper = new TransactionMapper();
    private final EmailService emailService;
    private final AccountRepository accountRepository;

    public TransactionServiceImpl(TransactionRepository transactionRepository,
                                  EmailService emailService,
                                  AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.emailService = emailService;
        this.accountRepository = accountRepository;
    }

    @Override
    public List<TransactionDTO> getAllTransactionsByAccountNumber(String accountNumber) {
        log.debug("Getting all transactions for account: {}", accountNumber);
        List<Transaction> transactions = transactionRepository
                .findBySourceAccount_AccountNumberOrTargetAccount_AccountNumber(accountNumber, accountNumber);

        List<TransactionDTO> transactionDTOs = transactions.parallelStream()
                .map(transactionMapper::toDto)
                .sorted((t1, t2) -> t2.getTransactionDate().compareTo(t1.getTransactionDate()))
                .collect(Collectors.toList());

        log.debug("Found {} transactions for account: {}", transactionDTOs.size(), accountNumber);
        return transactionDTOs;
    }

    public void sendBankStatementByEmail(String accountNumber) {
        log.info("Sending bank statement by email for account: {}", accountNumber);
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            log.warn("Account number is null or empty");
            throw new IllegalArgumentException("Account number must not be null or empty");
        }
        List<TransactionDTO> transactions = getAllTransactionsByAccountNumber(accountNumber);

        StringBuilder sb = new StringBuilder();
        sb.append("Bank Statement for Account: ").append(accountNumber).append("\n\n");

        for(TransactionDTO txn : transactions) {
            sb.append("Date: ").append(txn.getTransactionDate())
                    .append(", Type: ").append(txn.getTransactionType())
                    .append(", Amount: ").append(txn.getAmount())
                    .append("\n");
        }

        Account account = accountRepository.findByAccountNumber(accountNumber);
        if (account == null || account.getUser() == null) {
            log.warn("Account or user not found for account: {}", accountNumber);
            return;
        }
        String email = account.getUser().getEmail();
        emailService.sendEmail(email, "Your Bank Statement", sb.toString());
        log.info("Bank statement sent successfully to: {}", email);
    }

	@Override
	public Map<String, Object> getTransactionChartData(String accountNumber, Integer year, String month) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Integer> getAvailableYears(String accountNumber) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> getMonthlySummary(String accountNumber, Integer year) {
		// TODO Auto-generated method stub
		return null;
	}

}
package com.webapp.bankingportal.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.webapp.bankingportal.dto.TransactionDTO;
import com.webapp.bankingportal.service.TransactionService;
import com.webapp.bankingportal.util.LoggedinUser;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    
    private final TransactionService transactionService;
    
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }
    
    @GetMapping
    public ResponseEntity<List<TransactionDTO>> getAllTransactions() {
        String accountNumber = LoggedinUser.getAccountNumber();
        List<TransactionDTO> transactions = transactionService
                .getAllTransactionsByAccountNumber(accountNumber);
        return ResponseEntity.ok(transactions);
    }
    
    /**
     * API for line chart data
     */
    @GetMapping("/chart")
    public ResponseEntity<Map<String, Object>> getTransactionChartData(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String month) {
        
        String accountNumber = LoggedinUser.getAccountNumber();
        Map<String, Object> chartData = transactionService
                .getTransactionChartData(accountNumber, year, month);
        
        return ResponseEntity.ok(chartData);
    }
    
    /**
     * API for available years filter
     */
    @GetMapping("/years")
    public ResponseEntity<List<Integer>> getAvailableYears() {
        String accountNumber = LoggedinUser.getAccountNumber();
        List<Integer> years = transactionService.getAvailableYears(accountNumber);
        return ResponseEntity.ok(years);
    }
    
    /**
     * API for monthly summary (for bar chart)
     */
    @GetMapping("/monthly-summary")
    public ResponseEntity<Map<String, Object>> getMonthlySummary(
            @RequestParam(required = false) Integer year) {
        
        String accountNumber = LoggedinUser.getAccountNumber();
        
        // Default to current year if not specified
        if (year == null) {
            year = java.time.Year.now().getValue();
        }
        
        Map<String, Object> summary = transactionService
                .getMonthlySummary(accountNumber, year);
        
        return ResponseEntity.ok(summary);
    }
    
    @GetMapping("/send-statement")
    public ResponseEntity<String> sendBankStatement() {
        String accountNumber = LoggedinUser.getAccountNumber();
        transactionService.sendBankStatementByEmail(accountNumber);
        return ResponseEntity.ok("Bank statement sent to your email.");
    }
}
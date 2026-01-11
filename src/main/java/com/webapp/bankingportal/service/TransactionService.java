package com.webapp.bankingportal.service;

import com.webapp.bankingportal.dto.TransactionDTO;
import com.webapp.bankingportal.dto.ExpenseStatisticsDTO;
import java.util.List;
import java.util.Map;

public interface TransactionService {
    
    List<TransactionDTO> getAllTransactionsByAccountNumber(String accountNumber);
    
    // Thêm các methods mới
    Map<String, Object> getTransactionChartData(String accountNumber, Integer year, String month);
    
    List<Integer> getAvailableYears(String accountNumber);
    
    Map<String, Object> getMonthlySummary(String accountNumber, Integer year);
    
    void sendBankStatementByEmail(String accountNumber);
    
    ExpenseStatisticsDTO getExpenseStatistics(String accountNumber, Integer year);
}
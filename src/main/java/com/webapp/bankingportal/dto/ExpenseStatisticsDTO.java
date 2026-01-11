package com.webapp.bankingportal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class ExpenseStatisticsDTO {
    
    @JsonProperty("totalDeposits")
    private double totalDeposits;
    
    @JsonProperty("totalWithdrawals")
    private double totalWithdrawals;
    
    @JsonProperty("netBalance")
    private double netBalance; // deposits - withdrawals
    
    @JsonProperty("monthlyData")
    private List<MonthlyExpenseData> monthlyData;
    
    @JsonProperty("dailyData")
    private List<DailyExpenseData> dailyData;
    
    @JsonProperty("weeklyData")
    private List<WeeklyExpenseData> weeklyData;
    
    @JsonProperty("categoryData")
    private Map<String, Double> categoryData;
    
    // Constructors
    public ExpenseStatisticsDTO() {}
    
    public ExpenseStatisticsDTO(double totalDeposits, double totalWithdrawals, double netBalance) {
        this.totalDeposits = totalDeposits;
        this.totalWithdrawals = totalWithdrawals;
        this.netBalance = netBalance;
    }
    
    // Getters and Setters
    public double getTotalDeposits() {
        return totalDeposits;
    }
    
    public void setTotalDeposits(double totalDeposits) {
        this.totalDeposits = totalDeposits;
    }
    
    public double getTotalWithdrawals() {
        return totalWithdrawals;
    }
    
    public void setTotalWithdrawals(double totalWithdrawals) {
        this.totalWithdrawals = totalWithdrawals;
    }
    
    public double getNetBalance() {
        return netBalance;
    }
    
    public void setNetBalance(double netBalance) {
        this.netBalance = netBalance;
    }
    
    public List<MonthlyExpenseData> getMonthlyData() {
        return monthlyData;
    }
    
    public void setMonthlyData(List<MonthlyExpenseData> monthlyData) {
        this.monthlyData = monthlyData;
    }
    
    public List<DailyExpenseData> getDailyData() {
        return dailyData;
    }
    
    public void setDailyData(List<DailyExpenseData> dailyData) {
        this.dailyData = dailyData;
    }
    
    public List<WeeklyExpenseData> getWeeklyData() {
        return weeklyData;
    }
    
    public void setWeeklyData(List<WeeklyExpenseData> weeklyData) {
        this.weeklyData = weeklyData;
    }
    
    public Map<String, Double> getCategoryData() {
        return categoryData;
    }
    
    public void setCategoryData(Map<String, Double> categoryData) {
        this.categoryData = categoryData;
    }
    
    // Inner classes for structured data
    public static class MonthlyExpenseData {
        @JsonProperty("month")
        private String month;
        
        @JsonProperty("year")
        private int year;
        
        @JsonProperty("deposits")
        private double deposits;
        
        @JsonProperty("withdrawals")
        private double withdrawals;
        
        @JsonProperty("netBalance")
        private double netBalance;
        
        @JsonProperty("categoryData")
        private Map<String, Double> categoryData;
        
        public MonthlyExpenseData() {}
        
        public MonthlyExpenseData(String month, int year, double deposits, double withdrawals) {
            this.month = month;
            this.year = year;
            this.deposits = deposits;
            this.withdrawals = withdrawals;
            this.netBalance = deposits - withdrawals;
        }
        
        // Getters and Setters
        public String getMonth() {
            return month;
        }
        
        public void setMonth(String month) {
            this.month = month;
        }
        
        public int getYear() {
            return year;
        }
        
        public void setYear(int year) {
            this.year = year;
        }
        
        public double getDeposits() {
            return deposits;
        }
        
        public void setDeposits(double deposits) {
            this.deposits = deposits;
            this.netBalance = this.deposits - this.withdrawals;
        }
        
        public double getWithdrawals() {
            return withdrawals;
        }
        
        public void setWithdrawals(double withdrawals) {
            this.withdrawals = withdrawals;
            this.netBalance = this.deposits - this.withdrawals;
        }
        
        public double getNetBalance() {
            return netBalance;
        }
        
        public Map<String, Double> getCategoryData() {
            return categoryData;
        }
        
        public void setCategoryData(Map<String, Double> categoryData) {
            this.categoryData = categoryData;
        }
    }
    
    public static class DailyExpenseData {
        @JsonProperty("date")
        private String date;
        
        @JsonProperty("deposits")
        private double deposits;
        
        @JsonProperty("withdrawals")
        private double withdrawals;
        
        @JsonProperty("netBalance")
        private double netBalance;
        
        @JsonProperty("categoryData")
        private Map<String, Double> categoryData;
        
        public DailyExpenseData() {}
        
        public DailyExpenseData(String date, double deposits, double withdrawals) {
            this.date = date;
            this.deposits = deposits;
            this.withdrawals = withdrawals;
            this.netBalance = deposits - withdrawals;
        }
        
        // Getters and Setters
        public String getDate() {
            return date;
        }
        
        public void setDate(String date) {
            this.date = date;
        }
        
        public double getDeposits() {
            return deposits;
        }
        
        public void setDeposits(double deposits) {
            this.deposits = deposits;
            this.netBalance = this.deposits - this.withdrawals;
        }
        
        public double getWithdrawals() {
            return withdrawals;
        }
        
        public void setWithdrawals(double withdrawals) {
            this.withdrawals = withdrawals;
            this.netBalance = this.deposits - this.withdrawals;
        }
        
        public double getNetBalance() {
            return netBalance;
        }
        
        public Map<String, Double> getCategoryData() {
            return categoryData;
        }
        
        public void setCategoryData(Map<String, Double> categoryData) {
            this.categoryData = categoryData;
        }
    }
    
    public static class WeeklyExpenseData {
        @JsonProperty("week")
        private String week; // e.g., "Week 1, Jan 2024"
        
        @JsonProperty("deposits")
        private double deposits;
        
        @JsonProperty("withdrawals")
        private double withdrawals;
        
        @JsonProperty("netBalance")
        private double netBalance;
        
        @JsonProperty("categoryData")
        private Map<String, Double> categoryData;
        
        public WeeklyExpenseData() {}
        
        public WeeklyExpenseData(String week, double deposits, double withdrawals) {
            this.week = week;
            this.deposits = deposits;
            this.withdrawals = withdrawals;
            this.netBalance = deposits - withdrawals;
        }
        
        // Getters and Setters
        public String getWeek() {
            return week;
        }
        
        public void setWeek(String week) {
            this.week = week;
        }
        
        public double getDeposits() {
            return deposits;
        }
        
        public void setDeposits(double deposits) {
            this.deposits = deposits;
            this.netBalance = this.deposits - this.withdrawals;
        }
        
        public double getWithdrawals() {
            return withdrawals;
        }
        
        public void setWithdrawals(double withdrawals) {
            this.withdrawals = withdrawals;
            this.netBalance = this.deposits - this.withdrawals;
        }
        
        public double getNetBalance() {
            return netBalance;
        }
        
        public Map<String, Double> getCategoryData() {
            return categoryData;
        }
        
        public void setCategoryData(Map<String, Double> categoryData) {
            this.categoryData = categoryData;
        }
    }
}



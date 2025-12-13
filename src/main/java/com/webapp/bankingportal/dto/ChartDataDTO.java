package com.webapp.bankingportal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChartDataDTO {
    
    @JsonProperty("date")
    private String date;
    
    @JsonProperty("deposit")
    private double deposit;
    
    @JsonProperty("withdrawal")
    private double withdrawal;
    
    @JsonProperty("transfer")
    private double transfer;
    
    @JsonProperty("credit")
    private double credit;
    
    // Constructors
    public ChartDataDTO() {}
    
    public ChartDataDTO(String date, double deposit, double withdrawal, double transfer, double credit) {
        this.date = date;
        this.deposit = deposit;
        this.withdrawal = withdrawal;
        this.transfer = transfer;
        this.credit = credit;
    }
    
    // Getters and Setters
    public String getDate() {
        return date;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
    
    public double getDeposit() {
        return deposit;
    }
    
    public void setDeposit(double deposit) {
        this.deposit = deposit;
    }
    
    public double getWithdrawal() {
        return withdrawal;
    }
    
    public void setWithdrawal(double withdrawal) {
        this.withdrawal = withdrawal;
    }
    
    public double getTransfer() {
        return transfer;
    }
    
    public void setTransfer(double transfer) {
        this.transfer = transfer;
    }
    
    public double getCredit() {
        return credit;
    }
    
    public void setCredit(double credit) {
        this.credit = credit;
    }
}
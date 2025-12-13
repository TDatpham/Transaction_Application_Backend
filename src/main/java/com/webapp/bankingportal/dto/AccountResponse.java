package com.webapp.bankingportal.dto;

import com.webapp.bankingportal.entity.Account;

public class AccountResponse {

    private String accountNumber;
    private double balance;
    private String accountType;
    private String branch;
    private String ifscCode;

    public AccountResponse() {
    }

    public AccountResponse(String accountNumber, double balance, String accountType, String branch, String ifscCode) {
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.accountType = accountType;
        this.branch = branch;
        this.ifscCode = ifscCode;
    }

    public AccountResponse(Account account) {
        this.accountNumber = account.getAccountNumber();
        this.balance = account.getBalance();
        this.accountType = account.getAccountType();
        this.branch = account.getBranch();
        this.ifscCode = account.getIfscCode();
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getIfscCode() {
        return ifscCode;
    }

    public void setIfscCode(String ifscCode) {
        this.ifscCode = ifscCode;
    }

    @Override
    public String toString() {
        return "AccountResponse{" +
                "accountNumber='" + accountNumber + '\'' +
                ", balance=" + balance +
                ", accountType='" + accountType + '\'' +
                '}';
    }
}

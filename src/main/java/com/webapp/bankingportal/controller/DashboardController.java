package com.webapp.bankingportal.controller;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.webapp.bankingportal.service.DashboardService;
import com.webapp.bankingportal.util.JsonUtil;
import com.webapp.bankingportal.util.LoggedinUser;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/user")
    public ResponseEntity<String> getUserDetails() {
        String accountNumber = LoggedinUser.getAccountNumber();
        com.webapp.bankingportal.dto.UserResponse userResponse = dashboardService.getUserDetails(accountNumber);
        return ResponseEntity.ok(JsonUtil.toJson(userResponse));
    }

    @GetMapping("/account")
    public ResponseEntity<String> getAccountDetails() {
        String accountNumber = LoggedinUser.getAccountNumber();
        com.webapp.bankingportal.dto.AccountResponse accountResponse = dashboardService.getAccountDetails(accountNumber);
        return ResponseEntity.ok(JsonUtil.toJson(accountResponse));
    }

}

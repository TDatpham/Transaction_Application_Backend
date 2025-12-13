package com.webapp.bankingportal;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.webapp.bankingportal.exception.NotFoundException;
import com.webapp.bankingportal.service.DashboardService;

public class DashboardServiceTests extends BaseTest {

    @Autowired
    DashboardService dashboardService;

    @Test
    public void test_get_user_details_with_valid_account_number() throws Exception {
        Map<String, String> userDetails = createAndLoginUser();
        String accountNumber = userDetails.get("accountNumber");
        com.webapp.bankingportal.dto.UserResponse userResponse = dashboardService.getUserDetails(accountNumber);
        Assertions.assertNotNull(userResponse);
        Assertions.assertEquals(accountNumber, userResponse.getAccountNumber());
    }

    @Test
    public void test_get_user_details_with_invalid_account_number() throws Exception {
        String accountNumber = "123456789";
        Assertions.assertThrows(NotFoundException.class, () -> {
            dashboardService.getUserDetails(accountNumber);
        });
    }

    @Test
    public void test_get_account_details_with_valid_account_number() throws Exception {
        Map<String, String> userDetails = createAndLoginUser();
        String accountNumber = userDetails.get("accountNumber");
        com.webapp.bankingportal.dto.AccountResponse accountResponse = dashboardService.getAccountDetails(accountNumber);
        Assertions.assertNotNull(accountResponse);
        Assertions.assertEquals(accountNumber, accountResponse.getAccountNumber());
    }

    @Test
    public void test_get_account_details_with_invalid_account_number() throws Exception {
        String accountNumber = "123456789";
        Assertions.assertThrows(NotFoundException.class, () -> {
            dashboardService.getAccountDetails(accountNumber);
        });
    }

}
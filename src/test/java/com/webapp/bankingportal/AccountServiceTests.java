package com.webapp.bankingportal;

import jakarta.validation.ConstraintViolationException;

import java.util.HashMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import com.webapp.bankingportal.entity.User;
import com.webapp.bankingportal.exception.InsufficientBalanceException;
import com.webapp.bankingportal.exception.InvalidAmountException;
import com.webapp.bankingportal.exception.InvalidPinException;
import com.webapp.bankingportal.exception.NotFoundException;
import com.webapp.bankingportal.exception.UnauthorizedException;
import com.webapp.bankingportal.repository.AccountRepository;

public class AccountServiceTests extends BaseTest {

    @Autowired
    AccountRepository accountRepository;

    @Test
    public void test_create_account_with_valid_user() {
        User user = createUser();
        userRepository.save(user);

        com.webapp.bankingportal.entity.Account account = accountService.createAccount(user);

        Assertions.assertNotNull(account);
        Assertions.assertNotNull(account.getAccountNumber());
        Assertions.assertEquals(user, account.getUser());
        Assertions.assertEquals(0.0, account.getBalance());
    }

    @Test
    public void test_create_account_with_null_user() {
        Assertions.assertThrows(ConstraintViolationException.class, () -> accountService.createAccount(null));
    }

    @Test
    public void test_create_pin_with_valid_details() {
        java.util.HashMap<String, String> accountDetails = (HashMap<String, String>) createAccount();

        String pin = getRandomPin();

        accountService.createPin(accountDetails.get("accountNumber"), accountDetails.get("password"), pin);

        com.webapp.bankingportal.entity.Account account = accountRepository
                .findByAccountNumber(accountDetails.get("accountNumber"));

        Assertions.assertTrue(passwordEncoder.matches(pin, account.getPin()));
    }

    @Test
    public void test_create_pin_with_invalid_account_number() {
        Assertions.assertThrows(NotFoundException.class, () -> {
            accountService.createPin(getRandomAccountNumber(), getRandomPassword(), getRandomPin());
        });
    }

    @Test
    public void test_create_pin_with_invalid_password() {
        String accountNumber = createAccount()
                .get("accountNumber");

        Assertions.assertThrows(UnauthorizedException.class, () -> {
            accountService.createPin(accountNumber, getRandomPassword(), getRandomPin());
        });
    }

    @Test
    public void test_create_pin_with_existing_pin() {
        java.util.HashMap<String, String> accountDetails = (HashMap<String, String>) createAccountWithPin(
                passwordEncoder, userRepository, accountService);

        Assertions.assertThrows(UnauthorizedException.class, () -> {
            accountService.createPin(accountDetails.get("accountNumber"), accountDetails.get("password"),
                    getRandomPin());
        });

    }

    @Test
    public void test_create_pin_with_missing_or_empty_pin() {
        java.util.HashMap<String, String> accountDetails = (HashMap<String, String>) createAccount();

        Assertions.assertThrows(InvalidPinException.class, () -> {
            accountService.createPin(accountDetails.get("accountNumber"), accountDetails.get("password"), null);
        });

        Assertions.assertThrows(InvalidPinException.class, () -> {
            accountService.createPin(accountDetails.get("accountNumber"), accountDetails.get("password"), "");
        });
    }

    @Test
    public void test_create_pin_with_invalid_format() {
        java.util.HashMap<String, String> accountDetails = (HashMap<String, String>) createAccount();

        // Short pin
        Assertions.assertThrows(InvalidPinException.class, () -> {
            accountService.createPin(accountDetails.get("accountNumber"), accountDetails.get("password"),
                    faker.number().digits(3));
        });

        // Long pin
        Assertions.assertThrows(InvalidPinException.class, () -> {
            accountService.createPin(accountDetails.get("accountNumber"), accountDetails.get("password"),
                    faker.number().digits(5));
        });

        // Invalid format
        Assertions.assertThrows(InvalidPinException.class, () -> {
            accountService.createPin(accountDetails.get("accountNumber"), accountDetails.get("password"),
                    getRandomPassword().substring(0, 4));
        });
    }

    @Test
    public void test_update_pin_with_valid_details() {
        java.util.HashMap<String, String> accountDetails = (HashMap<String, String>) createAccountWithPin(
                passwordEncoder, userRepository, accountService);

        String newPin = getRandomPin();

        accountService.updatePin(accountDetails.get("accountNumber"), accountDetails.get("pin"),
                accountDetails.get("password"), newPin);

        com.webapp.bankingportal.entity.Account account = accountRepository
                .findByAccountNumber(accountDetails.get("accountNumber"));

        Assertions.assertTrue(passwordEncoder.matches(newPin, account.getPin()));
    }

    @Test
    public void test_update_pin_with_invalid_account_number() {
        Assertions.assertThrows(NotFoundException.class, () -> {
            accountService.updatePin(getRandomAccountNumber(), getRandomPin(), getRandomPassword(), getRandomPin());
        });
    }

    @Test
    public void test_update_pin_with_incorrect_password() {
        java.util.HashMap<String, String> accountDetails = (HashMap<String, String>) createAccountWithPin(
                passwordEncoder, userRepository, accountService);

        Assertions.assertThrows(UnauthorizedException.class, () -> {
            accountService.updatePin(accountDetails.get("accountNumber"), accountDetails.get("pin"),
                    getRandomPassword(), getRandomPin());
        });
    }

    @Test
    public void test_update_pin_with_missing_or_empty_password() {
        java.util.HashMap<String, String> accountDetails = (HashMap<String, String>) createAccountWithPin(
                passwordEncoder, userRepository, accountService);

        Assertions.assertThrows(UnauthorizedException.class, () -> {
            accountService.updatePin(accountDetails.get("accountNumber"), accountDetails.get("pin"), null,
                    getRandomPin());
        });

        Assertions.assertThrows(UnauthorizedException.class, () -> {
            accountService.updatePin(accountDetails.get("accountNumber"), accountDetails.get("pin"), "",
                    getRandomPin());
        });
    }

    @Test
    public void test_update_pin_with_incorrect_pin() {
        java.util.HashMap<String, String> accountDetails = (HashMap<String, String>) createAccountWithPin(
                passwordEncoder, userRepository, accountService);

        Assertions.assertThrows(UnauthorizedException.class, () -> {
            accountService.updatePin(accountDetails.get("accountNumber"), getRandomPin(),
                    accountDetails.get("password"), getRandomPin());
        });
    }

    @Test
    public void test_update_pin_for_account_with_no_pin() {
        java.util.HashMap<String, String> accountDetails = (HashMap<String, String>) createAccount();

        Assertions.assertThrows(UnauthorizedException.class, () -> {
            accountService.updatePin(accountDetails.get("accountNumber"), getRandomPin(),
                    accountDetails.get("password"), getRandomPin());
        });
    }

    @Test
    public void test_update_pin_with_missing_or_empty_old_pin() {
        java.util.HashMap<String, String> accountDetails = (HashMap<String, String>) createAccountWithPin(
                passwordEncoder, userRepository, accountService);

        Assertions.assertThrows(UnauthorizedException.class, () -> {
            accountService.updatePin(accountDetails.get("accountNumber"), null, accountDetails.get("password"),
                    getRandomPin());
        });

        Assertions.assertThrows(UnauthorizedException.class, () -> {
            accountService.updatePin(accountDetails.get("accountNumber"), "", accountDetails.get("password"),
                    getRandomPin());
        });
    }

    @Test
    public void test_update_pin_with_missing_or_empty_new_pin() {
        java.util.HashMap<String, String> accountDetails = (HashMap<String, String>) createAccountWithPin(
                passwordEncoder, userRepository, accountService);

        Assertions.assertThrows(InvalidPinException.class, () -> {
            accountService.updatePin(accountDetails.get("accountNumber"), accountDetails.get("pin"),
                    accountDetails.get("password"), null);
        });

        Assertions.assertThrows(InvalidPinException.class, () -> {
            accountService.updatePin(accountDetails.get("accountNumber"), accountDetails.get("pin"),
                    accountDetails.get("password"), "");
        });
    }

    @Test
    public void test_deposit_cash_with_valid_details() {
        double balance = 1000.0;
        java.util.HashMap<String, String> accountDetails = (HashMap<String, String>) createAccountWithInitialBalance(
                balance);

        com.webapp.bankingportal.entity.Account account = accountRepository
                .findByAccountNumber(accountDetails.get("accountNumber"));

        Assertions.assertEquals(balance, account.getBalance(), 0.01);
    }

    @Test
    public void test_deposit_cash_with_invalid_account_number() {
        Assertions.assertThrows(NotFoundException.class, () -> {
            accountService.cashDeposit(getRandomAccountNumber(), getRandomPin(), 50.0);
        });
    }

    @Test
    public void test_deposit_cash_with_invalid_pin() {
        java.util.HashMap<String, String> accountDetails = (HashMap<String, String>) createAccountWithPin(
                passwordEncoder, userRepository, accountService);

        Assertions.assertThrows(UnauthorizedException.class, () -> {
            accountService.cashDeposit(accountDetails.get("accountNumber"), getRandomPin(), 50.0);
        });
    }

    @Test
    public void test_deposit_invalid_amount() {
        java.util.HashMap<String, String> accountDetails = (HashMap<String, String>) createAccountWithPin(
                passwordEncoder, userRepository, accountService);

        // Negative amount
        Assertions.assertThrows(InvalidAmountException.class, () -> {
            accountService.cashDeposit(accountDetails.get("accountNumber"), accountDetails.get("pin"), -50.0);
        });

        // Zero amount
        Assertions.assertThrows(InvalidAmountException.class, () -> {
            accountService.cashDeposit(accountDetails.get("accountNumber"), accountDetails.get("pin"), 0.0);
        });

        // Amount not in multiples of 100
        Assertions.assertThrows(InvalidAmountException.class, () -> {
            accountService.cashDeposit(accountDetails.get("accountNumber"), accountDetails.get("pin"), 50.0);
        });

        // Amount greater than 100,000
        Assertions.assertThrows(InvalidAmountException.class, () -> {
            accountService.cashDeposit(accountDetails.get("accountNumber"), accountDetails.get("pin"), 100001.0);
        });
    }

    @Test
    public void test_withdraw_cash_with_valid_details() {
        double balance = 1000.0;
        java.util.HashMap<String, String> accountDetails = (HashMap<String, String>) createAccountWithInitialBalance(
                balance);

        double withdrawalAmount = 500.0;
        accountService.cashWithdrawal(accountDetails.get("accountNumber"), accountDetails.get("pin"), withdrawalAmount);

        com.webapp.bankingportal.entity.Account account = accountRepository
                .findByAccountNumber(accountDetails.get("accountNumber"));

        Assertions.assertEquals(balance - withdrawalAmount, account.getBalance(), 0.01);
    }

    @Test
    public void test_withdraw_insufficient_balance() {
        java.util.HashMap<String, String> accountDetails = (HashMap<String, String>) createAccountWithInitialBalance(
                500.0);

        Assertions.assertThrows(InsufficientBalanceException.class, () -> {
            accountService.cashWithdrawal(accountDetails.get("accountNumber"), accountDetails.get("pin"), 1000.0);
        });
    }

    @Test
    public void test_transfer_funds_with_valid_accounts() {
        double sourceAccountBalance = 1000.0;
        java.util.HashMap<String, String> sourceAccountDetails = (HashMap<String, String>) createAccountWithInitialBalance(
                sourceAccountBalance);

        double targetAccountBalance = 500.0;
        java.util.HashMap<String, String> targetAccountDetails = (HashMap<String, String>) createAccountWithInitialBalance(
                targetAccountBalance);

        int transferAmount = 200;
        accountService.fundTransfer(sourceAccountDetails.get("accountNumber"),
                targetAccountDetails.get("accountNumber"), sourceAccountDetails.get("pin"), transferAmount, null);

        com.webapp.bankingportal.entity.Account sourceAccount = accountRepository
                .findByAccountNumber(sourceAccountDetails.get("accountNumber"));
        com.webapp.bankingportal.entity.Account targetAccount = accountRepository
                .findByAccountNumber(targetAccountDetails.get("accountNumber"));

        Assertions.assertEquals(sourceAccountBalance - transferAmount, sourceAccount.getBalance(), 0.01);

        Assertions.assertEquals(targetAccountBalance + transferAmount, targetAccount.getBalance(), 0.01);
    }

    @Test
    public void test_transfer_non_existent_target_account() {
        java.util.HashMap<String, String> accountDetails = (HashMap<String, String>) createAccountWithInitialBalance(
                500.0);

        Assertions.assertThrows(NotFoundException.class, () -> {
            accountService.fundTransfer(accountDetails.get("accountNumber"), getRandomAccountNumber(),
                    accountDetails.get("pin"), 1000.0, null);
        });
    }

    @Test
    public void test_transfer_funds_insufficient_balance() {
        java.util.HashMap<String, String> sourceAccountDetails = (HashMap<String, String>) createAccountWithInitialBalance(
                500.0);

        java.util.HashMap<String, String> targetAccountDetails = (HashMap<String, String>) createAccount();

        Assertions.assertThrows(InsufficientBalanceException.class, () -> {
            accountService.fundTransfer(sourceAccountDetails.get("accountNumber"),
                    targetAccountDetails.get("accountNumber"), sourceAccountDetails.get("pin"), 1000.0, null);
        });
    }

}

package com.webapp.bankingportal.service;

import java.util.Date;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.webapp.bankingportal.entity.Account;
import com.webapp.bankingportal.entity.Transaction;
import com.webapp.bankingportal.entity.TransactionType;
import com.webapp.bankingportal.entity.User;
import com.webapp.bankingportal.exception.FundTransferException;
import com.webapp.bankingportal.exception.InsufficientBalanceException;
import com.webapp.bankingportal.exception.InvalidAmountException;
import com.webapp.bankingportal.exception.InvalidPinException;
import com.webapp.bankingportal.exception.NotFoundException;
import com.webapp.bankingportal.exception.UnauthorizedException;
import com.webapp.bankingportal.repository.AccountRepository;
import com.webapp.bankingportal.repository.TransactionRepository;
import com.webapp.bankingportal.util.ApiMessages;
import com.webapp.bankingportal.exception.UnauthorizedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AccountServiceImpl implements AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);

    @Autowired
    private final AccountRepository accountRepository;
    @Autowired
    private final PasswordEncoder passwordEncoder;
    @Autowired
    private final TransactionRepository transactionRepository;

    public AccountServiceImpl(AccountRepository accountRepository,
                              PasswordEncoder passwordEncoder,
                              TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Account createAccount(User user) {
        log.info("Creating account for user: {}", user.getEmail());
        Account account = new Account();
        account.setAccountNumber(generateUniqueAccountNumber());
        account.setBalance(0.0);
        account.setUser(user);
        Account savedAccount = accountRepository.save(account);
        log.info("Account created successfully: {}", savedAccount.getAccountNumber());
        return savedAccount;
    }

    @Override
    public boolean isPinCreated(String accountNumber) {
        log.debug("Checking if PIN is created for account: {}", accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber);
        if (account == null) {
            log.warn("Account not found: {}", accountNumber);
            throw new NotFoundException(ApiMessages.ACCOUNT_NOT_FOUND.getMessage());
        }

        return account.getPin() != null;
    }

    private String generateUniqueAccountNumber() {
        log.debug("Generating unique account number");
        String accountNumber;
        do {
            // Generate a UUID as the account number
            accountNumber = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 6);
        } while (accountRepository.findByAccountNumber(accountNumber) != null);

        log.debug("Generated account number: {}", accountNumber);
        return accountNumber;
    }

    private void validatePin(String accountNumber, String pin) {
        log.debug("Validating PIN for account: {}", accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber);
        if (account == null) {
            log.warn("Account not found: {}", accountNumber);
            throw new NotFoundException(ApiMessages.ACCOUNT_NOT_FOUND.getMessage());
        }

        if (account.getPin() == null) {
            log.warn("PIN not created for account: {}", accountNumber);
            throw new UnauthorizedException(ApiMessages.PIN_NOT_CREATED.getMessage());
        }

        if (pin == null || pin.isEmpty()) {
            log.warn("PIN is empty for account: {}", accountNumber);
            throw new UnauthorizedException(ApiMessages.PIN_EMPTY_ERROR.getMessage());
        }

        if (!passwordEncoder.matches(pin, account.getPin())) {
            log.warn("Invalid PIN for account: {}", accountNumber);
            throw new UnauthorizedException(ApiMessages.PIN_INVALID_ERROR.getMessage());
        }
    }

    private void validatePassword(String accountNumber, String password) {
        log.debug("Validating password for account: {}", accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber);
        if (account == null) {
            log.warn("Account not found: {}", accountNumber);
            throw new NotFoundException(ApiMessages.ACCOUNT_NOT_FOUND.getMessage());
        }

        if (password == null || password.isEmpty()) {
            log.warn("Password is empty for account: {}", accountNumber);
            throw new UnauthorizedException(ApiMessages.PASSWORD_EMPTY_ERROR.getMessage());
        }

        if (!passwordEncoder.matches(password, account.getUser().getPassword())) {
            log.warn("Invalid password for account: {}", accountNumber);
            throw new UnauthorizedException(ApiMessages.PASSWORD_INVALID_ERROR.getMessage());
        }
    }
    
    @Override
    public void createPin(String accountNumber, String password, String pin) {
        log.info("Creating PIN for account: {}", accountNumber);
        validatePassword(accountNumber, password);

        Account account = accountRepository.findByAccountNumber(accountNumber);
        if (account.getPin() != null) {
            log.warn("PIN already exists for account: {}", accountNumber);
            throw new UnauthorizedException(ApiMessages.PIN_ALREADY_EXISTS.getMessage());
        }

        if (pin == null || pin.isEmpty()) {
            log.warn("PIN is empty for account: {}", accountNumber);
            throw new InvalidPinException(ApiMessages.PIN_EMPTY_ERROR.getMessage());
        }

        if (!pin.matches("[0-9]{4}")) {
            log.warn("Invalid PIN format for account: {}", accountNumber);
            throw new InvalidPinException(ApiMessages.PIN_FORMAT_INVALID_ERROR.getMessage());
        }

        account.setPin(passwordEncoder.encode(pin));
        accountRepository.save(account);
        log.info("PIN created successfully for account: {}", accountNumber);
    }

    @Override
    public void updatePin(String accountNumber, String oldPin, String password, String newPin) {
        log.info("Updating PIN for account: {}", accountNumber);

        validatePassword(accountNumber, password);
        validatePin(accountNumber, oldPin);

        Account account = accountRepository.findByAccountNumber(accountNumber);

        if (newPin == null || newPin.isEmpty()) {
            log.warn("New PIN is empty for account: {}", accountNumber);
            throw new InvalidPinException(ApiMessages.PIN_EMPTY_ERROR.getMessage());
        }

        if (!newPin.matches("[0-9]{4}")) {
            log.warn("Invalid new PIN format for account: {}", accountNumber);
            throw new InvalidPinException(ApiMessages.PIN_FORMAT_INVALID_ERROR.getMessage());
        }

        account.setPin(passwordEncoder.encode(newPin));
        accountRepository.save(account);
        log.info("PIN updated successfully for account: {}", accountNumber);
    }

    private void validateAmount(double amount) {
        log.debug("Validating amount: {}", amount);
        if (amount <= 0) {
            log.warn("Amount is negative or zero: {}", amount);
            throw new InvalidAmountException(ApiMessages.AMOUNT_NEGATIVE_ERROR.getMessage());
        }

        if (amount % 100 != 0) {
            log.warn("Amount is not multiple of 100: {}", amount);
            throw new InvalidAmountException(ApiMessages.AMOUNT_NOT_MULTIPLE_OF_100_ERROR.getMessage());
        }

        if (amount > 100000) {
            log.warn("Amount exceeds limit: {}", amount);
            throw new InvalidAmountException(ApiMessages.AMOUNT_EXCEED_100_000_ERROR.getMessage());
        }
    }

    @Transactional
    @Override
    public void cashDeposit(String accountNumber, String pin, double amount) {
        log.info("Cash deposit request for account: {}, amount: {}", accountNumber, amount);
        validatePin(accountNumber, pin);
        validateAmount(amount);

        Account account = accountRepository.findByAccountNumber(accountNumber);
        double currentBalance = account.getBalance();
        double newBalance = currentBalance + amount;
        account.setBalance(newBalance);
        accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setTransactionType(TransactionType.CASH_DEPOSIT);
        transaction.setTransactionDate(new Date());
        transaction.setSourceAccount(account);
        transactionRepository.save(transaction);
        log.info("Cash deposit successful for account: {}, new balance: {}", accountNumber, newBalance);
    }

    @Transactional
    @Override
    public void cashWithdrawal(String accountNumber, String pin, double amount) {
        log.info("Cash withdrawal request for account: {}, amount: {}", accountNumber, amount);
        validatePin(accountNumber, pin);
        validateAmount(amount);

        Account account = accountRepository.findByAccountNumber(accountNumber);
        double currentBalance = account.getBalance();
        if (currentBalance < amount) {
            log.warn("Insufficient balance for account: {}, balance: {}, requested: {}", accountNumber, currentBalance, amount);
            throw new InsufficientBalanceException(ApiMessages.BALANCE_INSUFFICIENT_ERROR.getMessage());
        }

        double newBalance = currentBalance - amount;
        account.setBalance(newBalance);
        accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setTransactionType(TransactionType.CASH_WITHDRAWAL);
        transaction.setTransactionDate(new Date());
        transaction.setSourceAccount(account);
        transactionRepository.save(transaction);
        log.info("Cash withdrawal successful for account: {}, new balance: {}", accountNumber, newBalance);
    }

    @Transactional
    @Override
    public void fundTransfer(String sourceAccountNumber, String targetAccountNumber, String pin, double amount, String category) {
        log.info("Fund transfer request from account: {} to account: {}, amount: {}, category: {}", sourceAccountNumber, targetAccountNumber, amount, category);
        validatePin(sourceAccountNumber, pin);
        validateAmount(amount);

        if (sourceAccountNumber.equals(targetAccountNumber)) {
            log.warn("Source and target accounts are the same: {}", sourceAccountNumber);
            throw new FundTransferException(ApiMessages.CASH_TRANSFER_SAME_ACCOUNT_ERROR.getMessage());
        }

        Account targetAccount = accountRepository.findByAccountNumber(targetAccountNumber);
        if (targetAccount == null) {
            log.warn("Target account not found: {}", targetAccountNumber);
            throw new NotFoundException(ApiMessages.ACCOUNT_NOT_FOUND.getMessage());
        }

        Account sourceAccount = accountRepository.findByAccountNumber(sourceAccountNumber);
        double sourceBalance = sourceAccount.getBalance();
        if (sourceBalance < amount) {
            log.warn("Insufficient balance for transfer from account: {}, balance: {}, requested: {}", sourceAccountNumber, sourceBalance, amount);
            throw new InsufficientBalanceException(ApiMessages.BALANCE_INSUFFICIENT_ERROR.getMessage());
        }

        double newSourceBalance = sourceBalance - amount;
        sourceAccount.setBalance(newSourceBalance);
        accountRepository.save(sourceAccount);

        double targetBalance = targetAccount.getBalance();
        double newTargetBalance = targetBalance + amount;
        targetAccount.setBalance(newTargetBalance);
        accountRepository.save(targetAccount);

        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setTransactionType(TransactionType.CASH_TRANSFER);
        transaction.setTransactionDate(new Date());
        transaction.setSourceAccount(sourceAccount);
        transaction.setTargetAccount(targetAccount);
        transaction.setCategory(category);
        transactionRepository.save(transaction);
        log.info("Fund transfer successful from account: {} to account: {}, amount: {}, category: {}", sourceAccountNumber, targetAccountNumber, amount, category);
    }

}

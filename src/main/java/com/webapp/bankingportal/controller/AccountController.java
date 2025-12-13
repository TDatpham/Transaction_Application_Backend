/*
package com.webapp.bankingportal.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.cache.annotation.Cacheable;
import com.webapp.bankingportal.dto.AmountRequest;
import com.webapp.bankingportal.dto.FundTransferRequest;
import com.webapp.bankingportal.dto.PinRequest;
import com.webapp.bankingportal.dto.PinUpdateRequest;
import com.webapp.bankingportal.service.AccountService;
import com.webapp.bankingportal.service.TransactionService;
import com.webapp.bankingportal.util.ApiMessages;
import com.webapp.bankingportal.util.JsonUtil;
import com.webapp.bankingportal.util.LoggedinUser;


@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    public AccountController(AccountService accountService, TransactionService transactionService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    @GetMapping("/pin/check")
    public ResponseEntity<String> checkAccountPIN() {
        boolean isPINValid = accountService.isPinCreated(LoggedinUser.getAccountNumber());
        String response = isPINValid ? ApiMessages.PIN_CREATED.getMessage()
                : ApiMessages.PIN_NOT_CREATED.getMessage();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/pin/create")
    @Cacheable(value = "idempotency", key = "T(com.webapp.bankingportal.util.LoggedinUser).getAccountNumber() + ':' + '/api/account/pin/create' + ':' + (#pinRequest).hashCode()")
    public ResponseEntity<String> createPIN(@RequestBody PinRequest pinRequest) {
        accountService.createPin(
                LoggedinUser.getAccountNumber(),
                pinRequest.password(),
                pinRequest.pin());

        return ResponseEntity.ok(ApiMessages.PIN_CREATION_SUCCESS.getMessage());
    }

    @PostMapping("/pin/update")
    @Cacheable(value = "idempotency", key = "T(com.webapp.bankingportal.util.LoggedinUser).getAccountNumber() + ':' + '/api/account/pin/update' + ':' + (#pinUpdateRequest).hashCode()")
    public ResponseEntity<String> updatePIN(@RequestBody PinUpdateRequest pinUpdateRequest) {
        accountService.updatePin(
                LoggedinUser.getAccountNumber(),
                pinUpdateRequest.oldPin(),
                pinUpdateRequest.password(),
                pinUpdateRequest.newPin());

        return ResponseEntity.ok(ApiMessages.PIN_UPDATE_SUCCESS.getMessage());
    }

    @PostMapping("/deposit")
    @Cacheable(value = "idempotency", key = "T(com.webapp.bankingportal.util.LoggedinUser).getAccountNumber() + ':' + '/api/account/deposit' + ':' + (#amountRequest).hashCode()")
    public ResponseEntity<String> cashDeposit(@RequestBody AmountRequest amountRequest) {
        accountService.cashDeposit(
                LoggedinUser.getAccountNumber(),
                amountRequest.pin(),
                amountRequest.amount());

        return ResponseEntity.ok(ApiMessages.CASH_DEPOSIT_SUCCESS.getMessage());
    }

    @PostMapping("/withdraw")
    @Cacheable(value = "idempotency", key = "T(com.webapp.bankingportal.util.LoggedinUser).getAccountNumber() + ':' + '/api/account/withdraw' + ':' + (#amountRequest).hashCode()")
    public ResponseEntity<String> cashWithdrawal(@RequestBody AmountRequest amountRequest) {
        accountService.cashWithdrawal(
                LoggedinUser.getAccountNumber(),
                amountRequest.pin(),
                amountRequest.amount());

        return ResponseEntity.ok(ApiMessages.CASH_WITHDRAWAL_SUCCESS.getMessage());
    }

    @PostMapping("/fund-transfer")
    @Cacheable(value = "idempotency", key = "T(com.webapp.bankingportal.util.LoggedinUser).getAccountNumber() + ':' + '/api/account/fund-transfer' + ':' + (#fundTransferRequest).hashCode()")
    public ResponseEntity<String> fundTransfer(@RequestBody FundTransferRequest fundTransferRequest) {
        accountService.fundTransfer(
                LoggedinUser.getAccountNumber(),
                fundTransferRequest.targetAccountNumber(),
                fundTransferRequest.pin(),
                fundTransferRequest.amount());

        return ResponseEntity.ok(ApiMessages.CASH_TRANSFER_SUCCESS.getMessage());
    }

    @GetMapping("/transactions")
    public ResponseEntity<String> getAllTransactionsByAccountNumber() {
        java.util.List<com.webapp.bankingportal.dto.TransactionDTO> transactions = transactionService
                .getAllTransactionsByAccountNumber(LoggedinUser.getAccountNumber());
        return ResponseEntity.ok(JsonUtil.toJson(transactions));
    }
    @GetMapping("/send-statement")
    public ResponseEntity<String> sendBankStatement() {
        String accountNumber = LoggedinUser.getAccountNumber(); // Get logged-in user account
        transactionService.sendBankStatementByEmail(accountNumber);
        return ResponseEntity.ok("{\"message\": \"Bank statement sent to your email.\"}");
    }

}
*/
package com.webapp.bankingportal.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.webapp.bankingportal.dto.*;
import com.webapp.bankingportal.exception.UnauthorizedException;
import com.webapp.bankingportal.service.AccountService;
import com.webapp.bankingportal.service.TransactionService;
import com.webapp.bankingportal.service.CacheService;
import com.webapp.bankingportal.type.CacheKeyType;
import com.webapp.bankingportal.util.ApiMessages;
import com.webapp.bankingportal.util.JsonUtil;
import com.webapp.bankingportal.util.LoggedinUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private static final Logger log = LoggerFactory.getLogger(AccountController.class);
    
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final CacheService cacheService;

    public AccountController(AccountService accountService, 
                            TransactionService transactionService,
                            CacheService cacheService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.cacheService = cacheService;
    }

    @GetMapping("/pin/check")
    public ResponseEntity<String> checkAccountPIN() {
        boolean isPINValid = accountService.isPinCreated(LoggedinUser.getAccountNumber());
        String response = isPINValid ? ApiMessages.PIN_CREATED.getMessage()
                : ApiMessages.PIN_NOT_CREATED.getMessage();
        return ResponseEntity.ok(response);
    }
    
    /*
    @PostMapping("/pin/create")
    public ResponseEntity<?> createPIN(@RequestBody PinRequest pinRequest) {
        try {
            // Kiểm tra idempotency
            String idempotencyKey = generateIdempotencyKey("pin/create", pinRequest);
            
            if (cacheService.exists(CacheKeyType.IDEMPOTENCY, idempotencyKey)) {
                log.warn("Duplicate PIN creation request for account: {}", 
                        LoggedinUser.getAccountNumber());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of(
                            "success", false,
                            "message", "PIN creation request already processed"
                        ));
            }
            
            // Xử lý tạo PIN
            accountService.createPin(
                    LoggedinUser.getAccountNumber(),
                    pinRequest.password(),
                    pinRequest.pin());
            
            // Lưu vào cache idempotency (5 phút)
            cacheService.put(CacheKeyType.IDEMPOTENCY, "processed", 300, idempotencyKey);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", ApiMessages.PIN_CREATION_SUCCESS.getMessage()
            ));
            
        } catch (Exception e) {
            log.error("Failed to create PIN", e);
            return ResponseEntity.badRequest()
                    .body(Map.of(
                        "success", false,
                        "message", e.getMessage()
                    ));
        }
    }
	*/
    
    @PostMapping("/pin/create")
    public ResponseEntity<?> createPIN(@RequestBody PinRequest pinRequest) {
        try {
            log.info("Creating PIN for account: {}", LoggedinUser.getAccountNumber());
            
            // Tạm thời bỏ qua cache check nếu Redis lỗi
            try {
                String idempotencyKey = LoggedinUser.getAccountNumber() + 
                                       ":pin-create:" + 
                                       pinRequest.hashCode();
                
                if (cacheService.exists(CacheKeyType.IDEMPOTENCY, idempotencyKey)) {
                    log.warn("Duplicate PIN creation request for account: {}", 
                            LoggedinUser.getAccountNumber());
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of(
                                "success", false,
                                "message", "PIN creation request already processed"
                            ));
                }
            } catch (Exception e) {
                log.warn("Cache service unavailable, continuing without cache check");
            }
            
            // Kiểm tra PIN đã tồn tại chưa (từ database)
            boolean pinExists = accountService.isPinCreated(LoggedinUser.getAccountNumber());
            if (pinExists) {
                log.warn("PIN already exists for account: {}", LoggedinUser.getAccountNumber());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                            "success", false,
                            "message", "PIN already created for this account"
                        ));
            }
            
            // Tạo PIN
            accountService.createPin(
                    LoggedinUser.getAccountNumber(),
                    pinRequest.password(),
                    pinRequest.pin());
            
            // Thử lưu cache nhưng không bắt lỗi
            try {
                String idempotencyKey = LoggedinUser.getAccountNumber() + 
                                       ":pin-create:" + 
                                       pinRequest.hashCode();
                cacheService.put(CacheKeyType.IDEMPOTENCY, "processed", 300, idempotencyKey);
            } catch (Exception e) {
                log.warn("Failed to store in cache, but PIN creation successful");
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", ApiMessages.PIN_CREATION_SUCCESS.getMessage()
            ));
            
        } catch (UnauthorizedException e) {
            log.error("PIN creation unauthorized: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "success", false,
                        "message", e.getMessage()
                    ));
        } catch (Exception e) {
            log.error("Failed to create PIN", e);
            return ResponseEntity.badRequest()
                    .body(Map.of(
                        "success", false,
                        "message", "Failed to create PIN: " + e.getMessage()
                    ));
        }
    }
    
    @PostMapping("/pin/update")
    public ResponseEntity<?> updatePIN(@RequestBody PinUpdateRequest pinUpdateRequest) {
        try {
            String idempotencyKey = generateIdempotencyKey("pin/update", pinUpdateRequest);
            
            if (cacheService.exists(CacheKeyType.IDEMPOTENCY, idempotencyKey)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of(
                            "success", false,
                            "message", "PIN update request already processed"
                        ));
            }
            
            accountService.updatePin(
                    LoggedinUser.getAccountNumber(),
                    pinUpdateRequest.oldPin(),
                    pinUpdateRequest.password(),
                    pinUpdateRequest.newPin());
            
            cacheService.put(CacheKeyType.IDEMPOTENCY, "processed", 300, idempotencyKey);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", ApiMessages.PIN_UPDATE_SUCCESS.getMessage()
            ));
            
        } catch (Exception e) {
            log.error("Failed to update PIN", e);
            return ResponseEntity.badRequest()
                    .body(Map.of(
                        "success", false,
                        "message", e.getMessage()
                    ));
        }
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> cashDeposit(@RequestBody AmountRequest amountRequest) {
        try {
            String idempotencyKey = generateIdempotencyKey("deposit", amountRequest);
            
            if (cacheService.exists(CacheKeyType.IDEMPOTENCY, idempotencyKey)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of(
                            "success", false,
                            "message", "Deposit request already processed"
                        ));
            }
            
            accountService.cashDeposit(
                    LoggedinUser.getAccountNumber(),
                    amountRequest.pin(),
                    amountRequest.amount());
            
            cacheService.put(CacheKeyType.IDEMPOTENCY, "processed", 300, idempotencyKey);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", ApiMessages.CASH_DEPOSIT_SUCCESS.getMessage()
            ));
            
        } catch (Exception e) {
            log.error("Failed to deposit cash", e);
            return ResponseEntity.badRequest()
                    .body(Map.of(
                        "success", false,
                        "message", e.getMessage()
                    ));
        }
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> cashWithdrawal(@RequestBody AmountRequest amountRequest) {
        try {
            String idempotencyKey = generateIdempotencyKey("withdraw", amountRequest);
            
            if (cacheService.exists(CacheKeyType.IDEMPOTENCY, idempotencyKey)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of(
                            "success", false,
                            "message", "Withdrawal request already processed"
                        ));
            }
            
            accountService.cashWithdrawal(
                    LoggedinUser.getAccountNumber(),
                    amountRequest.pin(),
                    amountRequest.amount());
            
            cacheService.put(CacheKeyType.IDEMPOTENCY, "processed", 300, idempotencyKey);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", ApiMessages.CASH_WITHDRAWAL_SUCCESS.getMessage()
            ));
            
        } catch (Exception e) {
            log.error("Failed to withdraw cash", e);
            return ResponseEntity.badRequest()
                    .body(Map.of(
                        "success", false,
                        "message", e.getMessage()
                    ));
        }
    }

    @PostMapping("/fund-transfer")
    public ResponseEntity<?> fundTransfer(@RequestBody FundTransferRequest fundTransferRequest) {
        try {
            String idempotencyKey = generateIdempotencyKey("fund-transfer", fundTransferRequest);
            
            if (cacheService.exists(CacheKeyType.IDEMPOTENCY, idempotencyKey)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of(
                            "success", false,
                            "message", "Fund transfer request already processed"
                        ));
            }
            
            accountService.fundTransfer(
                    LoggedinUser.getAccountNumber(),
                    fundTransferRequest.targetAccountNumber(),
                    fundTransferRequest.pin(),
                    fundTransferRequest.amount());
            
            cacheService.put(CacheKeyType.IDEMPOTENCY, "processed", 300, idempotencyKey);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", ApiMessages.CASH_TRANSFER_SUCCESS.getMessage()
            ));
            
        } catch (Exception e) {
            log.error("Failed to transfer funds", e);
            return ResponseEntity.badRequest()
                    .body(Map.of(
                        "success", false,
                        "message", e.getMessage()
                    ));
        }
    }

    @GetMapping("/transactions")
    public ResponseEntity<String> getAllTransactionsByAccountNumber() {
        java.util.List<com.webapp.bankingportal.dto.TransactionDTO> transactions = transactionService
                .getAllTransactionsByAccountNumber(LoggedinUser.getAccountNumber());
        return ResponseEntity.ok(JsonUtil.toJson(transactions));
    }
    
    @GetMapping("/send-statement")
    public ResponseEntity<String> sendBankStatement() {
        String accountNumber = LoggedinUser.getAccountNumber();
        transactionService.sendBankStatementByEmail(accountNumber);
        return ResponseEntity.ok("{\"message\": \"Bank statement sent to your email.\"}");
    }
    
    /**
     * Tạo idempotency key
     */
    private String generateIdempotencyKey(String endpoint, Object request) {
        String accountNumber = LoggedinUser.getAccountNumber();
        int requestHash = request != null ? request.hashCode() : 0;
        
        return String.format("%s:%s:%d", 
            accountNumber, 
            endpoint, 
            requestHash);
    }
}
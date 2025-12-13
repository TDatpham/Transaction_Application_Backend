package com.webapp.bankingportal.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import com.webapp.bankingportal.entity.OtpInfo;
import com.webapp.bankingportal.exception.AccountDoesNotExistException;
import com.webapp.bankingportal.exception.InvalidOtpException;
import com.webapp.bankingportal.exception.OtpRetryLimitExceededException;
import com.webapp.bankingportal.repository.OtpInfoRepository;
import com.webapp.bankingportal.util.ValidationUtil;
import com.webapp.bankingportal.util.ApiMessages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OtpServiceImpl implements OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpServiceImpl.class);

    public static final int OTP_ATTEMPTS_LIMIT = 3;
    public static final int OTP_EXPIRY_MINUTES = 5;
    public static final int OTP_RESET_WAITING_TIME_MINUTES = 10;
    public static final int OTP_RETRY_LIMIT_WINDOW_MINUTES = 15;

    private final CacheManager cacheManager;
    private final EmailService emailService;
    private final OtpInfoRepository otpInfoRepository;
    private final ValidationUtil validationUtil;

    private LocalDateTime otpLimitReachedTime = null;

    public OtpServiceImpl(CacheManager cacheManager,
                         EmailService emailService,
                         OtpInfoRepository otpInfoRepository,
                         ValidationUtil validationUtil) {
        this.cacheManager = cacheManager;
        this.emailService = emailService;
        this.otpInfoRepository = otpInfoRepository;
        this.validationUtil = validationUtil;
    }

    @Override
    public String generateOTP(String accountNumber) {
        log.info("Generating OTP for account: {}", accountNumber);
        if (!validationUtil.doesAccountExist(accountNumber)) {
            log.warn("Account does not exist: {}", accountNumber);
            throw new AccountDoesNotExistException(ApiMessages.ACCOUNT_NOT_FOUND.getMessage());
        }

        OtpInfo existingOtpInfo = otpInfoRepository.findByAccountNumber(accountNumber);
        if (existingOtpInfo == null) {
            incrementOtpAttempts(accountNumber);
            return generateNewOTP(accountNumber);
        }

        validateOtpWithinRetryLimit(existingOtpInfo);

        if (isOtpExpired(existingOtpInfo)) {
            log.debug("Existing OTP expired, generating new OTP for account: {}", accountNumber);
            return generateNewOTP(accountNumber);
        }

        // Existing OTP is not expired
        existingOtpInfo.setGeneratedAt(LocalDateTime.now());
        incrementOtpAttempts(accountNumber);
        log.info("Reusing existing OTP for account: {}", accountNumber);
        return existingOtpInfo.getOtp();
    }

    private void validateOtpWithinRetryLimit(OtpInfo otpInfo) {
        if (!isOtpRetryLimitExceeded(otpInfo)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        if (otpLimitReachedTime == null) {
            otpLimitReachedTime = now;
        }

        long waitingMinutes = OTP_RESET_WAITING_TIME_MINUTES - otpLimitReachedTime.until(now, ChronoUnit.MINUTES);

        log.warn("OTP retry limit exceeded for account: {}, waiting minutes: {}", otpInfo.getAccountNumber(), waitingMinutes);
        throw new OtpRetryLimitExceededException(
                String.format(ApiMessages.OTP_GENERATION_LIMIT_EXCEEDED.getMessage(), waitingMinutes));
    }

    private boolean isOtpRetryLimitExceeded(OtpInfo otpInfo) {
        int attempts = getOtpAttempts(otpInfo.getAccountNumber());
        if (attempts < OTP_ATTEMPTS_LIMIT) {
            return false;
        }

        if (isOtpResetWaitingTimeExceeded()) {
            resetOtpAttempts(otpInfo.getAccountNumber());
            return false;
        }

        LocalDateTime now = LocalDateTime.now();

        return otpInfo.getGeneratedAt().isAfter(now.minusMinutes(OTP_RETRY_LIMIT_WINDOW_MINUTES));
    }

    private boolean isOtpResetWaitingTimeExceeded() {
        LocalDateTime now = LocalDateTime.now();
        return otpLimitReachedTime != null
                && otpLimitReachedTime.isBefore(now.minusMinutes(OTP_RESET_WAITING_TIME_MINUTES));
    }

    private void incrementOtpAttempts(String accountNumber) {
        if (!validationUtil.doesAccountExist(accountNumber)) {
            throw new AccountDoesNotExistException(ApiMessages.ACCOUNT_NOT_FOUND.getMessage());
        }

        Cache cache = cacheManager.getCache("otpAttempts");
        if (cache != null) {
            cache.put(accountNumber, getOtpAttempts(accountNumber) + 1);
        }
    }

    private void resetOtpAttempts(String accountNumber) {
        log.debug("Resetting OTP attempts for account: {}", accountNumber);
        otpLimitReachedTime = null;
        Cache cache = cacheManager.getCache("otpAttempts");
        if (cache != null) {
            cache.put(accountNumber, 0);
        }
    }

    private int getOtpAttempts(String accountNumber) {
        int otpAttempts = 0;
        Cache cache = cacheManager.getCache("otpAttempts");
        if (cache == null) {
            return otpAttempts;
        }

        Integer value = cache.get(accountNumber, Integer.class);
        if (value != null) {
            otpAttempts = value;
        }

        return otpAttempts;
    }

    private String generateNewOTP(String accountNumber) {
        log.debug("Generating new OTP for account: {}", accountNumber);
        Random random = new Random();
        int otpValue = 100_000 + random.nextInt(900_000);
        String otp = String.valueOf(otpValue);

        otpInfoRepository.save(new OtpInfo(accountNumber, otp, LocalDateTime.now()));
        log.info("New OTP generated for account: {}", accountNumber);
        return otp;
    }

    @Override
    public CompletableFuture<Void> sendOTPByEmail(String email, String name, String accountNumber, String otp) {
        log.info("Sending OTP by email to: {} for account: {}", email, accountNumber);
        String emailText = emailService.getOtpLoginEmailTemplate(name, "xxx" + accountNumber.substring(3), otp);
        return emailService.sendEmail(email, ApiMessages.EMAIL_SUBJECT_OTP.getMessage(), emailText);
    }

    @Override
    public boolean validateOTP(String accountNumber, String otp) {
        log.debug("Validating OTP for account: {}", accountNumber);
        OtpInfo otpInfo = otpInfoRepository.findByAccountNumberAndOtp(accountNumber, otp);
        if (otpInfo == null) {
            log.warn("Invalid OTP for account: {}", accountNumber);
            throw new InvalidOtpException(ApiMessages.OTP_INVALID_ERROR.getMessage());
        }

        boolean isValid = !isOtpExpired(otpInfo);
        if (isValid) {
            log.info("OTP validated successfully for account: {}", accountNumber);
        } else {
            log.warn("OTP expired for account: {}", accountNumber);
        }
        return isValid;
    }

    private boolean isOtpExpired(OtpInfo otpInfo) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime generatedAt = otpInfo.getGeneratedAt();
        boolean expired = generatedAt.isBefore(now.minusMinutes(OTP_EXPIRY_MINUTES));
        if (expired) {
            log.debug("OTP expired, deleting from repository for account: {}", otpInfo.getAccountNumber());
            otpInfoRepository.delete(otpInfo);
        }

        return expired;
    }

}

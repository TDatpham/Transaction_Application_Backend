package com.webapp.bankingportal.service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.webapp.bankingportal.dto.OtpRequest;
import com.webapp.bankingportal.dto.OtpVerificationRequest;
import com.webapp.bankingportal.dto.ResetPasswordRequest;
import com.webapp.bankingportal.entity.PasswordResetToken;
import com.webapp.bankingportal.entity.User;
import com.webapp.bankingportal.repository.PasswordResetTokenRepository;
import com.webapp.bankingportal.util.ApiMessages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private static final int EXPIRATION_HOURS = 24;

    private final OtpService otpService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserService userService;

    public AuthServiceImpl(OtpService otpService,
                          PasswordResetTokenRepository passwordResetTokenRepository,
                          UserService userService) {
        this.otpService = otpService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.userService = userService;
    }

    @Override
    public String generatePasswordResetToken(User user) {
        log.info("Generating password reset token for user: {}", user.getEmail());
        PasswordResetToken existingToken = passwordResetTokenRepository.findByUser(user);
        if (isExistingTokenValid(existingToken)) {
            log.debug("Reusing existing valid token for user: {}", user.getEmail());
            return existingToken.getToken();
        }

        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDateTime = LocalDateTime.now().plusHours(EXPIRATION_HOURS);
        PasswordResetToken resetToken = new PasswordResetToken(token, user, expiryDateTime);
        passwordResetTokenRepository.save(resetToken);
        log.info("Password reset token generated successfully for user: {}", user.getEmail());
        return token;
    }

    @Override
    public boolean verifyPasswordResetToken(String token, User user) {
        log.debug("Verifying password reset token for user: {}", user.getEmail());
        return passwordResetTokenRepository.findByToken(token)
                .map(resetToken -> {
                    deletePasswordResetToken(token);
                    boolean isValid = user.equals(resetToken.getUser()) && resetToken.isTokenValid();
                    if (isValid) {
                        log.info("Password reset token verified successfully for user: {}", user.getEmail());
                    } else {
                        log.warn("Password reset token invalid for user: {}", user.getEmail());
                    }
                    return isValid;
                })
                .orElse(false);
    }

    @Override
    public void deletePasswordResetToken(String token) {
        log.debug("Deleting password reset token");
        passwordResetTokenRepository.deleteByToken(token);
    }

    @Override
    public ResponseEntity<String> sendOtpForPasswordReset(OtpRequest otpRequest) {
        log.info("Received OTP request for password reset, identifier: {}", otpRequest.identifier());
        User user = userService.getUserByIdentifier(otpRequest.identifier());
        String accountNumber = user.getAccount().getAccountNumber();
        String generatedOtp = otpService.generateOTP(accountNumber);

        return sendOtpEmail(user, accountNumber, generatedOtp);
    }

    @Override
    public ResponseEntity<String> verifyOtpAndIssueResetToken(OtpVerificationRequest otpVerificationRequest) {
        log.info("Verifying OTP and issuing reset token for identifier: {}", otpVerificationRequest.identifier());
        validateOtpRequest(otpVerificationRequest);
        User user = userService.getUserByIdentifier(otpVerificationRequest.identifier());
        String accountNumber = user.getAccount().getAccountNumber();

        if (!otpService.validateOTP(accountNumber, otpVerificationRequest.otp())) {
            log.warn("OTP validation failed for identifier: {}", otpVerificationRequest.identifier());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiMessages.OTP_INVALID_ERROR.getMessage());
        }

        String resetToken = generatePasswordResetToken(user);
        log.info("Reset token issued successfully for identifier: {}", otpVerificationRequest.identifier());
        return ResponseEntity.ok(String.format(ApiMessages.PASSWORD_RESET_TOKEN_ISSUED.getMessage(), resetToken));
    }

    @Override
    @Transactional
    public ResponseEntity<String> resetPassword(ResetPasswordRequest resetPasswordRequest) {
        log.info("Password reset request for identifier: {}", resetPasswordRequest.identifier());
        User user = userService.getUserByIdentifier(resetPasswordRequest.identifier());

        if (!verifyPasswordResetToken(resetPasswordRequest.resetToken(), user)) {
            log.warn("Invalid reset token for identifier: {}", resetPasswordRequest.identifier());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiMessages.TOKEN_INVALID_ERROR.getMessage());
        }

        try {
            boolean passwordResetSuccessful = userService.resetPassword(user, resetPasswordRequest.newPassword());
            if (passwordResetSuccessful) {
                log.info("Password reset successful for identifier: {}", resetPasswordRequest.identifier());
                return ResponseEntity.ok(ApiMessages.PASSWORD_RESET_SUCCESS.getMessage());
            } else {
                log.error("Password reset failed for identifier: {}", resetPasswordRequest.identifier());
                return ResponseEntity.internalServerError().body(ApiMessages.PASSWORD_RESET_FAILURE.getMessage());
            }
        } catch (Exception e) {
            log.error("Error resetting password for user: {}", user.getEmail(), e);
            return ResponseEntity.internalServerError().body(ApiMessages.PASSWORD_RESET_FAILURE.getMessage());
        }
    }

    private boolean isExistingTokenValid(PasswordResetToken existingToken) {
        return existingToken != null && existingToken.getExpiryDateTime().isAfter(LocalDateTime.now().plusMinutes(5));
    }

    private ResponseEntity<String> sendOtpEmail(User user, String accountNumber, String generatedOtp) {
        log.debug("Sending OTP email for password reset to: {}", user.getEmail());
        CompletableFuture<Void> emailSendingFuture = otpService.sendOTPByEmail(user.getEmail(), user.getName(), accountNumber,
                generatedOtp);

        ResponseEntity<String> successResponse = ResponseEntity
                .ok(String.format(ApiMessages.OTP_SENT_SUCCESS.getMessage(), user.getEmail()));
        ResponseEntity<String> failureResponse = ResponseEntity.internalServerError()
                .body(String.format(ApiMessages.OTP_SENT_FAILURE.getMessage(), user.getEmail()));

        return emailSendingFuture.thenApply(result -> successResponse)
                .exceptionally(e -> {
                    log.error("Failed to send OTP email to: {}", user.getEmail(), e);
                    return failureResponse;
                }).join();
    }

    private void validateOtpRequest(OtpVerificationRequest otpVerificationRequest) {
        if (otpVerificationRequest.identifier() == null || otpVerificationRequest.identifier().isEmpty()) {
            log.warn("OTP request validation failed: missing identifier");
            throw new IllegalArgumentException(ApiMessages.IDENTIFIER_MISSING_ERROR.getMessage());
        }
        if (otpVerificationRequest.otp() == null || otpVerificationRequest.otp().isEmpty()) {
            log.warn("OTP request validation failed: missing OTP");
            throw new IllegalArgumentException(ApiMessages.OTP_MISSING_ERROR.getMessage());
        }
    }

}

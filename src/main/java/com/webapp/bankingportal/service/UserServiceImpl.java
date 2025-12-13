package com.webapp.bankingportal.service;

import java.sql.Timestamp;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;

import com.webapp.bankingportal.dto.LoginRequest;
import com.webapp.bankingportal.dto.OtpRequest;
import com.webapp.bankingportal.dto.OtpVerificationRequest;
import com.webapp.bankingportal.dto.UserResponse;
import com.webapp.bankingportal.entity.User;
import com.webapp.bankingportal.exception.InvalidTokenException;
import com.webapp.bankingportal.exception.PasswordResetException;
import com.webapp.bankingportal.exception.UnauthorizedException;
import com.webapp.bankingportal.exception.UserInvalidException;
import com.webapp.bankingportal.repository.UserRepository;
import com.webapp.bankingportal.util.JsonUtil;
import com.webapp.bankingportal.util.LoggedinUser;
import com.webapp.bankingportal.util.ValidationUtil;
import com.webapp.bankingportal.util.ApiMessages;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final AccountService accountService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final GeolocationService geolocationService;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final ValidationUtil validationUtil;
    
    @Autowired
    public UserServiceImpl(AccountService accountService,
                          AuthenticationManager authenticationManager,
                          EmailService emailService,
                          GeolocationService geolocationService,
                          OtpService otpService,
                          PasswordEncoder passwordEncoder,
                          TokenService tokenService,
                          UserDetailsService userDetailsService,
                          UserRepository userRepository,
                          ValidationUtil validationUtil) {
        this.accountService = accountService;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
        this.geolocationService = geolocationService;
        this.otpService = otpService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
        this.validationUtil = validationUtil;
    }

    @Override
    public ResponseEntity<String> registerUser(User user) {
        log.info("Registering new user with email: {}", user.getEmail());
        validationUtil.validateNewUser(user);
        encodePassword(user);
        User savedUser = saveUserWithAccount(user);
        log.info("User registered successfully with account number: {}", savedUser.getAccount().getAccountNumber());
        return ResponseEntity.ok(JsonUtil.toJson(new UserResponse(savedUser)));
    }

    @Override
    public ResponseEntity<String> login(LoginRequest loginRequest, HttpServletRequest request)
            throws InvalidTokenException {
        log.info("Login attempt for identifier: {}", loginRequest.identifier());
        User user = authenticateUser(loginRequest);
        sendLoginNotification(user, request.getRemoteAddr());
        String token = generateAndSaveToken(user.getAccount().getAccountNumber());
        log.info("User logged in successfully: {}", user.getAccount().getAccountNumber());
        return ResponseEntity.ok(String.format(ApiMessages.TOKEN_ISSUED_SUCCESS.getMessage(), token));
    }

    @Override
    public ResponseEntity<String> generateOtp(OtpRequest otpRequest) {
        log.info("Generating OTP for identifier: {}", otpRequest.identifier());
        User user = getUserByIdentifier(otpRequest.identifier());
        String otp = otpService.generateOTP(user.getAccount().getAccountNumber());
        log.info("OTP generated successfully for account: {}", user.getAccount().getAccountNumber());
        return sendOtpEmail(user, otp);
    }

    @Override
    public ResponseEntity<String> verifyOtpAndLogin(OtpVerificationRequest otpVerificationRequest)
            throws InvalidTokenException {
        log.info("Verifying OTP for identifier: {}", otpVerificationRequest.identifier());
        validateOtpRequest(otpVerificationRequest);
        User user = getUserByIdentifier(otpVerificationRequest.identifier());
        validateOtp(user, otpVerificationRequest.otp());
        String token = generateAndSaveToken(user.getAccount().getAccountNumber());
        log.info("OTP verified and user logged in: {}", user.getAccount().getAccountNumber());
        return ResponseEntity.ok(String.format(ApiMessages.TOKEN_ISSUED_SUCCESS.getMessage(), token));
    }

    @Override
    public ResponseEntity<String> updateUser(User updatedUser) {
        String accountNumber = LoggedinUser.getAccountNumber();
        log.info("Updating user for account: {}", accountNumber);
        authenticateUser(accountNumber, updatedUser.getPassword());
        User existingUser = getUserByAccountNumber(accountNumber);
        updateUserDetails(existingUser, updatedUser);
        User savedUser = saveUser(existingUser);
        log.info("User updated successfully for account: {}", accountNumber);
        return ResponseEntity.ok(JsonUtil.toJson(new UserResponse(savedUser)));
    }

    @Override
    @Transactional
    public boolean resetPassword(User user, String newPassword) {
        log.info("Resetting password for user: {}", user.getEmail());
        try {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            log.info("Password reset successfully for user: {}", user.getEmail());
            return true;
        } catch (Exception e) {
            log.error("Failed to reset password for user: {}", user.getEmail(), e);
            throw new PasswordResetException(ApiMessages.PASSWORD_RESET_FAILURE.getMessage(), e);
        }
    }

    @Override
    public ModelAndView logout(String token) throws InvalidTokenException {
        log.info("Logout request received");
        token = token.substring(7);
        tokenService.validateToken(token);
        tokenService.invalidateToken(token);
        String username = tokenService.getUsernameFromToken(token);
        log.info("User logged out successfully: {}", username);
        return new ModelAndView("redirect:/logout");
    }

    @Override
    public User saveUser(User user) {
        log.debug("Saving user: {}", user.getEmail());
        return userRepository.save(user);
    }

    @Override
    public User getUserByIdentifier(String identifier) {
        log.debug("Getting user by identifier: {}", identifier);
        User user = null;

        if (validationUtil.doesEmailExist(identifier)) {
            user = getUserByEmail(identifier);
        } else if (validationUtil.doesAccountExist(identifier)) {
            user = getUserByAccountNumber(identifier);
        } else {
            log.warn("User not found for identifier: {}", identifier);
            throw new UserInvalidException(
                    String.format(ApiMessages.USER_NOT_FOUND_BY_IDENTIFIER.getMessage(), identifier));
        }

        return user;
    }

    @Override
    public User getUserByAccountNumber(String accountNo) {
        log.debug("Getting user by account number: {}", accountNo);
        return userRepository.findByAccountAccountNumber(accountNo).orElseThrow(
                () -> new UserInvalidException(
                        String.format(ApiMessages.USER_NOT_FOUND_BY_ACCOUNT.getMessage(), accountNo)));
    }

    @Override
    public User getUserByEmail(String email) {
        log.debug("Getting user by email: {}", email);
        return userRepository.findByEmail(email).orElseThrow(
                () -> new UserInvalidException(String.format(ApiMessages.USER_NOT_FOUND_BY_EMAIL.getMessage(), email)));
    }

    private void encodePassword(User user) {
        log.debug("Encoding password for user: {}", user.getEmail());
        user.setCountryCode(user.getCountryCode().toUpperCase());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
    }

    private User saveUserWithAccount(User user) {
        log.debug("Saving user with account: {}", user.getEmail());
        User savedUser = saveUser(user);
        savedUser.setAccount(accountService.createAccount(savedUser));
        return saveUser(savedUser);
    }

    private User authenticateUser(LoginRequest loginRequest) {
        log.debug("Authenticating user with identifier: {}", loginRequest.identifier());
        User user = getUserByIdentifier(loginRequest.identifier());
        String accountNumber = user.getAccount().getAccountNumber();
        authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(accountNumber, loginRequest.password()));
        return user;
    }

    private void authenticateUser(String accountNumber, String password) {
        log.debug("Authenticating user with account number: {}", accountNumber);
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(accountNumber, password));
    }

    private String generateAndSaveToken(String accountNumber) throws InvalidTokenException {
        log.debug("Generating and saving token for account: {}", accountNumber);
        UserDetails userDetails = userDetailsService.loadUserByUsername(accountNumber);
        String token = tokenService.generateToken(userDetails);
        tokenService.saveToken(token);
        return token;
    }

    private ResponseEntity<String> sendOtpEmail(User user, String otp) {
        log.debug("Sending OTP email to: {}", user.getEmail());
        CompletableFuture<Void> emailSendingFuture = otpService.sendOTPByEmail(
                user.getEmail(), user.getName(), user.getAccount().getAccountNumber(), otp);

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

    private void validateOtpRequest(OtpVerificationRequest request) {
        if (request.identifier() == null || request.identifier().isEmpty()) {
            log.warn("OTP request validation failed: missing identifier");
            throw new IllegalArgumentException(ApiMessages.IDENTIFIER_MISSING_ERROR.getMessage());
        }
        if (request.otp() == null || request.otp().isEmpty()) {
            log.warn("OTP request validation failed: missing OTP");
            throw new IllegalArgumentException(ApiMessages.OTP_MISSING_ERROR.getMessage());
        }
    }

    private void validateOtp(User user, String otp) {
        log.debug("Validating OTP for user: {}", user.getEmail());
        if (!otpService.validateOTP(user.getAccount().getAccountNumber(), otp)) {
            log.warn("Invalid OTP for user: {}", user.getEmail());
            throw new UnauthorizedException(ApiMessages.OTP_INVALID_ERROR.getMessage());
        }
    }

    private void updateUserDetails(User existingUser, User updatedUser) {
        log.debug("Updating user details for: {}", existingUser.getEmail());
        
        // Kiểm tra và validate thông tin user
        if (updatedUser.getName() != null && !updatedUser.getName().trim().isEmpty()) {
            existingUser.setName(updatedUser.getName().trim());
        }
        
        if (updatedUser.getEmail() != null && !updatedUser.getEmail().trim().isEmpty()) {
            // Validate email format
            if (validationUtil.isValidEmail(updatedUser.getEmail())) {
                existingUser.setEmail(updatedUser.getEmail().trim());
            } else {
                // Sử dụng message mặc định thay vì EMAIL_INVALID_ERROR không tồn tại
                throw new IllegalArgumentException("Invalid email format: " + updatedUser.getEmail());
            }
        }
        
        if (updatedUser.getAddress() != null && !updatedUser.getAddress().trim().isEmpty()) {
            existingUser.setAddress(updatedUser.getAddress().trim());
        }
        
        if (updatedUser.getCountryCode() != null && !updatedUser.getCountryCode().trim().isEmpty()) {
            existingUser.setCountryCode(updatedUser.getCountryCode().trim().toUpperCase());
        }
        
        if (updatedUser.getPhoneNumber() != null && !updatedUser.getPhoneNumber().trim().isEmpty()) {
            existingUser.setPhoneNumber(updatedUser.getPhoneNumber().trim());
        }
        
        // Giữ nguyên password
        updatedUser.setPassword(existingUser.getPassword());
    }

    private void sendLoginNotification(User user, String ip) {
        log.debug("Sending login notification for user: {} from IP: {}", user.getEmail(), ip);
        
        // Chạy bất đồng bộ, không chờ kết quả
        CompletableFuture.runAsync(() -> {
            try {
                sendLoginNotificationAsync(user, ip).join();
            } catch (Exception e) {
                log.warn("Login notification failed for user: {}", user.getEmail(), e);
            }
        });
    }

    private CompletableFuture<Boolean> sendLoginNotificationAsync(User user, String ip) {
        log.debug("Sending login notification for user: {} from IP: {}", user.getEmail(), ip);
        String loginTime = new Timestamp(System.currentTimeMillis()).toString();

        return geolocationService.getGeolocation(ip)
                .thenComposeAsync(geolocationResponse -> {
                    // SỬA: Sử dụng method helper từ DTO
                    String loginLocation = geolocationResponse.getFormattedLocation();
                    return sendLoginEmail(user, loginTime, loginLocation);
                })
                .exceptionallyComposeAsync(throwable -> {
                    log.warn("Failed to get geolocation for IP: {}, using Unknown location", ip, throwable);
                    return sendLoginEmail(user, loginTime, "Unknown location");
                });
    }

    private CompletableFuture<Boolean> sendLoginEmail(User user, String loginTime, String loginLocation) {
        log.debug("Sending login email to: {}", user.getEmail());
        String emailText = emailService.getLoginEmailTemplate(user.getName(), loginTime, loginLocation);
        return emailService.sendEmail(user.getEmail(), ApiMessages.EMAIL_SUBJECT_LOGIN.getMessage(), emailText)
                .thenApplyAsync(result -> {
                    log.info("Login email sent successfully to: {}", user.getEmail());
                    return true;
                })
                .exceptionally(ex -> {
                    log.error("Failed to send login email to: {}", user.getEmail(), ex);
                    return false;
                });
    }

}
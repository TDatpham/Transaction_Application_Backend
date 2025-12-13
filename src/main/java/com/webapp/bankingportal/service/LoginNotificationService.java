package com.webapp.bankingportal.service;

import com.webapp.bankingportal.entity.User;
import com.webapp.bankingportal.dto.GeolocationResponse;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.concurrent.CompletableFuture;

@Service
public class LoginNotificationService {
    
    private static final Logger log = LoggerFactory.getLogger(LoginNotificationService.class);
    private final GeolocationService geolocationService;
    private final EmailService emailService;
    
    public LoginNotificationService(GeolocationService geolocationService, 
                                   EmailService emailService) {
        this.geolocationService = geolocationService;
        this.emailService = emailService;
    }
    
    @Async
    public CompletableFuture<Boolean> sendLoginNotificationAsync(User user, String ip) {
        log.debug("Sending login notification for user: {} from IP: {}", user.getEmail(), ip);
        String loginTime = new Timestamp(System.currentTimeMillis()).toString();

        return geolocationService.getGeolocation(ip)
                .thenComposeAsync(geolocationResponse -> {
                    // Sửa lỗi ở đây
                    String loginLocation = geolocationResponse.getFormattedLocation();
                    return sendLoginEmail(user, loginTime, loginLocation);
                })
                .exceptionallyComposeAsync(throwable -> {
                    log.warn("Failed to get geolocation for IP: {}, using Unknown location", ip, throwable);
                    return sendLoginEmail(user, loginTime, "Unknown location");
                });
    }
    
    private CompletableFuture<Boolean> sendLoginEmail(User user, String loginTime, String location) {
        // Gửi email logic
        return emailService.sendLoginNotificationEmail(user.getEmail(), loginTime, location);
    }
}
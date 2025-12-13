package com.webapp.bankingportal.service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.webapp.bankingportal.dto.GeolocationResponse;
import com.webapp.bankingportal.exception.GeolocationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class GeolocationServiceImpl implements GeolocationService {

    private static final Logger log = LoggerFactory.getLogger(GeolocationServiceImpl.class);

    @Value("${geo.api.url}")
    private String apiUrl;

    @Value("${geo.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public GeolocationServiceImpl() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    @Async
    public CompletableFuture<GeolocationResponse> getGeolocation(String ip) {
        log.info("Getting geolocation for IP: {}", ip);
        
        // Xử lý đặc biệt cho localhost và private IPs
        if (isLocalhostOrPrivate(ip)) {
            log.info("IP {} is localhost/private, returning default location", ip);
            return CompletableFuture.completedFuture(createDefaultResponse(ip));
        }

        try {
            // Validate IP address
            InetAddress.getByName(ip);
            log.debug("IP address validated: {}", ip);

            // Xây dựng URL đúng
            String url = buildGeolocationUrl(ip);
            log.debug("Calling geolocation API: {}", url);

            // Gọi API
            GeolocationResponse response = restTemplate.getForObject(url, GeolocationResponse.class);

            if (response == null) {
                log.error("Null response from geolocation API for IP: {}", ip);
                return CompletableFuture.completedFuture(createDefaultResponse(ip));
            }
            
            // Kiểm tra response từ ip-api.com
            if (response.getStatus() != null && "fail".equals(response.getStatus())) {
                log.warn("Geolocation API failed for IP: {} - Message: {}", 
                        ip, response.getMessage());
                return CompletableFuture.completedFuture(createDefaultResponse(ip));
            }

            log.info("Geolocation retrieved successfully for IP: {}", ip);
            return CompletableFuture.completedFuture(response);

        } catch (UnknownHostException e) {
            log.error("Invalid IP address: {}", ip, e);
            return CompletableFuture.completedFuture(createDefaultResponse(ip));
            
        } catch (RestClientException e) {
            log.error("Geolocation API error for IP: {} - {}", ip, e.getMessage());
            return CompletableFuture.completedFuture(createDefaultResponse(ip));
            
        } catch (Exception e) {
            log.error("Unexpected error getting geolocation for IP: {}", ip, e);
            return CompletableFuture.completedFuture(createDefaultResponse(ip));
        }
    }

    /**
     * Xây dựng URL cho geolocation API
     */
    private String buildGeolocationUrl(String ip) {
        // Thay thế {ip} trong URL template
        String url = apiUrl.replace("{ip}", ip);
        
        // Chỉ thêm token nếu có (cho các API cần token)
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            if (url.contains("?")) {
                url += "&token=" + apiKey.trim();
            } else {
                url += "?token=" + apiKey.trim();
            }
        }
        
        return url;
    }

    /**
     * Kiểm tra địa chỉ localhost/private
     */
    private boolean isLocalhostOrPrivate(String ip) {
        if (ip == null || ip.isEmpty()) {
            return true;
        }
        
        // IPv4 localhost
        if (ip.equals("127.0.0.1") || ip.equalsIgnoreCase("localhost")) {
            return true;
        }
        
        // IPv6 localhost
        if (ip.equals("0:0:0:0:0:0:0:1") || ip.equals("::1")) {
            return true;
        }
        
        // Private network ranges
        if (ip.startsWith("192.168.") || 
            ip.startsWith("10.") || 
            ip.startsWith("172.16.") || ip.startsWith("172.17.") || 
            ip.startsWith("172.18.") || ip.startsWith("172.19.") ||
            ip.startsWith("172.20.") || ip.startsWith("172.21.") ||
            ip.startsWith("172.22.") || ip.startsWith("172.23.") ||
            ip.startsWith("172.24.") || ip.startsWith("172.25.") ||
            ip.startsWith("172.26.") || ip.startsWith("172.27.") ||
            ip.startsWith("172.28.") || ip.startsWith("172.29.") ||
            ip.startsWith("172.30.") || ip.startsWith("172.31.")) {
            return true;
        }
        
        return false;
    }

    /**
     * Tạo response mặc định
     */
    private GeolocationResponse createDefaultResponse(String ip) {
        GeolocationResponse response = new GeolocationResponse();
        response.setQuery(ip);
        response.setStatus("success");
        response.setCity("Local");
        response.setRegionName("Network");
        response.setCountry("Local Network");
        response.setCountryCode("");
        response.setTimezone("");
        response.setIsp("Local ISP");
        return response;
    }
}
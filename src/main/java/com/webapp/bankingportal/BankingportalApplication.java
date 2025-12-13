package com.webapp.bankingportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableCaching // Add this annotation to enable caching support
@EnableAsync
@SpringBootApplication
@ComponentScan(basePackages = {"com.webapp.bankingportal"})
public class BankingportalApplication {
    public static void main(String[] args) {
        SpringApplication.run(BankingportalApplication.class, args);
    }
}

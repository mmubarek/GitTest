package com.example.api.token_api_spring_boot.config;

import com.example.secureapp.TokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TokenServiceConfig {

    @Value("${app.token.secret-key}")
    private String secretKey;

    @Value("${app.token.expiry-minutes:30}") // Default to 30 if not set
    private long expiryMinutes;

    @Bean
    public TokenService tokenService() {
        // Ensure properties are loaded. A better way might be constructor injection into the config class.
        if (secretKey == null || secretKey.isEmpty()) {
            throw new IllegalStateException("Token secret key is not configured (app.token.secret-key)");
        }
        return new TokenService(secretKey, expiryMinutes);
    }
}
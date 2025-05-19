package com.example.api.token_api_spring_boot.controller;

import com.example.api.token_api_spring_boot.dto.TokenGenerationRequest;
import com.example.api.token_api_spring_boot.dto.TokenResponse;
import com.example.api.token_api_spring_boot.dto.TokenValidationRequest;
import com.example.api.token_api_spring_boot.dto.ValidationResponse;
import com.example.secureapp.InvalidTokenException;
import com.example.secureapp.TokenService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/token")
public class TokenApiController {

    private static final Logger log = LoggerFactory.getLogger(TokenApiController.class);

    private final TokenService tokenService;
    private final Counter tokenValidationSuccessCounter;
    private final Counter tokenValidationFailureCounter;

    public TokenApiController(TokenService tokenService, MeterRegistry meterRegistry) {
        this.tokenService = tokenService;
        log.info("Injected MeterRegistry type: {}", meterRegistry.getClass().getName());

        this.tokenValidationSuccessCounter = Counter.builder("app.token.validation.status")
            .tag("result", "success")
            .description("Counts successful token validations")
            .register(meterRegistry);

        this.tokenValidationFailureCounter = Counter.builder("app.token.validation.status")
            .tag("result", "failure")
            .description("Counts failed token validations")
            .register(meterRegistry);
    }

    @PostMapping("/generate")
    public ResponseEntity<TokenResponse> generateToken(@Valid @RequestBody TokenGenerationRequest request) {
        // Use getter since TokenGenerationRequest is a class
        String token = tokenService.generateToken(request.getUserId()); // CHANGED from request.userId()
        return ResponseEntity.ok(new TokenResponse(token));
    }

    @PostMapping("/validate")
    public ResponseEntity<ValidationResponse> validateToken(@Valid @RequestBody TokenValidationRequest request) {
        // Use getter since TokenValidationRequest is (presumably) also a class
        String tokenToValidate = request.getToken(); // CHANGED from request.token()

        try {
            tokenService.validateToken(tokenToValidate);
            log.info("Token validation successful for token prefix: {}",
                tokenToValidate != null ? tokenToValidate.substring(0, Math.min(tokenToValidate.length(), 10)) + "..." : "null");
            tokenValidationSuccessCounter.increment();
            return ResponseEntity.ok(new ValidationResponse("valid"));
        } catch (InvalidTokenException e) {
            log.warn("Token validation failed for token prefix: {} - Reason: {}",
                tokenToValidate != null ? tokenToValidate.substring(0, Math.min(tokenToValidate.length(), 10)) + "..." : "null",
                e.getMessage());
            tokenValidationFailureCounter.increment();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                 .body(new ValidationResponse("invalid", e.getMessage()));
        }
    }
}
// src/test/java/com/example/secureapp/TokenServiceIncrementalTest.java
package com.example.secureapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * These tests augment the basic TokenServiceTest with more realistic
 * scenarios discussed in the Testing lecture (Performance, Robustness, Boundary).
 */
public class TokenServiceIncrementalTest {

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
    }

    @Test
    @DisplayName("Performance: Validate 10,000 tokens within 1 second (Latency check)")
    void performance_validateManyTokens_completesWithinTime() throws InvalidTokenException {
        String token = tokenService.generateToken("perfUser");
        
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            tokenService.validateToken(token);
        }
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("Validated 10,000 tokens in: " + duration + "ms");
        assertTrue(duration < 1000, "Validation is too slow! Took " + duration + "ms");
    }

    @Test
    @DisplayName("Robustness: Test with 'fuzzed' inputs (Special Characters/Injection)")
    void robustness_fuzzedInputs_throwsInvalidTokenException() {
        String[] payloads = {
            "garbage-data",
            "!!!@@@###$$$",
            "' OR 1=1 --",
            "<script>alert(1)</script>",
            "A".repeat(1000) // Large payload
        };

        for (String payload : payloads) {
            String encoded = Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
            assertThrows(InvalidTokenException.class, () -> {
                tokenService.validateToken(encoded);
            }, "Should fail for payload: " + payload);
        }
    }

    @Test
    @DisplayName("Boundary: Token with very short expiry (Testing limit)")
    void boundary_tokenAboutToExpire_stillValid() throws InvalidTokenException {
        // We simulate a token that expires 5 seconds in the future
        long futureTime = System.currentTimeMillis() + 5000;
        String data = "boundaryUser:" + futureTime;
        String signature = tokenService.calculateSignature(data);
        String token = Base64.getEncoder().encodeToString((data + ":" + signature).getBytes(StandardCharsets.UTF_8));

        assertTrue(tokenService.validateToken(token), "Token should still be valid 5s before expiry");
    }

    @Test
    @DisplayName("White-box: Tamper with internal signature only")
    void whiteBox_tamperSignature_fails() {
        String validToken = tokenService.generateToken("user");
        String decoded = new String(Base64.getDecoder().decode(validToken), StandardCharsets.UTF_8);
        String[] parts = decoded.split(":");
        
        // Change one character in the signature
        String tamperedSignature = parts[2].substring(0, parts[2].length() - 1) + (parts[2].endsWith("0") ? "1" : "0");
        String tamperedToken = Base64.getEncoder().encodeToString(
            (parts[0] + ":" + parts[1] + ":" + tamperedSignature).getBytes(StandardCharsets.UTF_8)
        );

        assertThrows(InvalidTokenException.class, () -> {
            tokenService.validateToken(tamperedToken);
        });
    }
}

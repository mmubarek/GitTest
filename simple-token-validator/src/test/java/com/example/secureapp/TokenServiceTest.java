// src/test/java/com/example/secureapp/TokenServiceTest.java
package com.example.secureapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TokenServiceTest {

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
    }

    @Test
    void generateToken_validUserId_returnsNonEmptyToken() {
        String token = tokenService.generateToken("testUser");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void generateToken_nullUserId_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            tokenService.generateToken(null);
        });
    }

    @Test
    void generateToken_emptyUserId_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            tokenService.generateToken(" ");
        });
    }

    @Test
    void validateToken_validToken_returnsTrue() throws InvalidTokenException {
        String token = tokenService.generateToken("testUser123");
        assertTrue(tokenService.validateToken(token));
    }

    @Test
    void validateToken_nullToken_throwsInvalidTokenException() {
        InvalidTokenException exception = assertThrows(InvalidTokenException.class, () -> {
            tokenService.validateToken(null);
        });
        assertEquals("Token cannot be null or empty.", exception.getMessage());
    }

    @Test
    void validateToken_emptyToken_throwsInvalidTokenException() {
         InvalidTokenException exception = assertThrows(InvalidTokenException.class, () -> {
            tokenService.validateToken("  ");
        });
        assertEquals("Token cannot be null or empty.", exception.getMessage());
    }

    @Test
    void validateToken_invalidBase64Token_throwsInvalidTokenException() {
        InvalidTokenException exception = assertThrows(InvalidTokenException.class, () -> {
            tokenService.validateToken("thisIsNotBase64!");
        });
        assertEquals("Token is not valid Base64.", exception.getMessage());
    }

    @Test
    void validateToken_malformedToken_throwsInvalidTokenException() {
        // Valid Base64 but wrong content structure
        String malformedToken = java.util.Base64.getEncoder().encodeToString("user:onlytwo_parts".getBytes());
        InvalidTokenException exception = assertThrows(InvalidTokenException.class, () -> {
            tokenService.validateToken(malformedToken);
        });
        assertEquals("Token format is invalid.", exception.getMessage());
    }

    @Test
    void validateToken_tamperedToken_throwsInvalidTokenException() {
        String originalToken = tokenService.generateToken("userToTamper");
        // Decode, tamper, re-encode
        String decoded = new String(java.util.Base64.getDecoder().decode(originalToken));
        String[] parts = decoded.split(":");
        // Change userId part
        String tamperedDecoded = "tamperedUser" + ":" + parts[1] + ":" + parts[2];
        String tamperedToken = java.util.Base64.getEncoder().encodeToString(tamperedDecoded.getBytes());

        InvalidTokenException exception = assertThrows(InvalidTokenException.class, () -> {
            tokenService.validateToken(tamperedToken);
        });
        assertEquals("Token signature is invalid.", exception.getMessage());
    }

    @Test
    void validateToken_expiredToken_throwsInvalidTokenException() throws InterruptedException {
        // Generate a token with effectively 0 expiry for testing
        // This is tricky with current TokenService, would need modification for precise expiry control
        // For now, let's simulate by generating token then waiting.
        // In a real scenario, you might inject a Clock or allow custom expiry for testing.

        // For this demo, we'll rely on the default 30 min and assume it won't expire during test.
        // To *actually* test expiry, one would need to:
        // 1. Modify TokenService to accept expiry duration
        // 2. Or, generate a token, then manually manipulate its expiry timestamp part after decoding
        // Let's try option 2 for this test
        String token = tokenService.generateToken("expiryUser");
        String decoded = new String(java.util.Base64.getDecoder().decode(token));
        String[] parts = decoded.split(":");
        long pastTime = System.currentTimeMillis() - (60 * 60 * 1000); // 1 hour ago
        String expiredDataToSign = parts[0] + ":" + pastTime;
        String newSignatureForExpired = tokenService.calculateSignature(expiredDataToSign); // Need access to private method or make it protected/public for test
        String expiredToken = java.util.Base64.getEncoder().encodeToString((expiredDataToSign + ":" + newSignatureForExpired).getBytes());


        InvalidTokenException exception = assertThrows(InvalidTokenException.class, () -> {
            tokenService.validateToken(expiredToken);
        });
        assertEquals("Token has expired.", exception.getMessage());
    }
}
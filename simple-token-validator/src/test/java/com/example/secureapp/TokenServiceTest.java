package com.example.secureapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets; // Added for explicit charset usage
import static org.junit.jupiter.api.Assertions.*;

public class TokenServiceTest {

    private TokenService tokenService;
    private static final String TEST_SECRET_KEY = "testSecretKeyForUnitTests";
    private static final long TEST_EXPIRY_MINUTES = 5; // A positive expiry for standard tests

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(TEST_SECRET_KEY, TEST_EXPIRY_MINUTES);
    }

    // Constructor Tests
    @Test
    void constructor_nullSecretKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new TokenService(null, 30L));
    }

    @Test
    void constructor_emptySecretKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new TokenService("   ", 30L));
    }

    @Test
    void constructor_zeroExpiry_throwsIllegalArgumentException() {
        // This test now verifies the constructor's behavior.
        assertThrows(IllegalArgumentException.class, () -> new TokenService("secret", 0L));
    }

     @Test
    void constructor_negativeExpiry_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new TokenService("secret", -5L));
    }

    // generateToken Tests
    @Test
    void generateToken_validUserId_returnsNonEmptyToken() {
        String token = tokenService.generateToken("testUser");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void generateToken_nullUserId_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> tokenService.generateToken(null));
    }

    @Test
    void generateToken_emptyUserId_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> tokenService.generateToken(" "));
    }

    // validateToken Tests
    @Test
    void validateToken_validToken_returnsTrue() throws InvalidTokenException {
        String token = tokenService.generateToken("testUser123");
        assertTrue(tokenService.validateToken(token));
    }

    @Test
    void validateToken_nullToken_throwsInvalidTokenException() {
        InvalidTokenException exception = assertThrows(InvalidTokenException.class, () -> tokenService.validateToken(null));
        assertEquals("Token cannot be null or empty.", exception.getMessage());
    }

    @Test
    void validateToken_emptyToken_throwsInvalidTokenException() {
         InvalidTokenException exception = assertThrows(InvalidTokenException.class, () -> tokenService.validateToken("  "));
        assertEquals("Token cannot be null or empty.", exception.getMessage());
    }

    @Test
    void validateToken_invalidBase64Token_throwsInvalidTokenException() {
        InvalidTokenException exception = assertThrows(InvalidTokenException.class, () -> tokenService.validateToken("thisIsNotBase64!"));
        assertEquals("Token is not valid Base64.", exception.getMessage());
    }

    @Test
    void validateToken_malformedToken_throwsInvalidTokenException() {
        String malformedToken = java.util.Base64.getEncoder().encodeToString("user:onlytwo_parts".getBytes(StandardCharsets.UTF_8));
        InvalidTokenException exception = assertThrows(InvalidTokenException.class, () -> tokenService.validateToken(malformedToken));
        assertEquals("Token format is invalid.", exception.getMessage());
    }

    @Test
    void validateToken_tamperedToken_throwsInvalidTokenException() {
        String originalToken = tokenService.generateToken("userToTamper");
        String decoded = new String(java.util.Base64.getDecoder().decode(originalToken), StandardCharsets.UTF_8);
        String[] parts = decoded.split(":");
        String tamperedDecoded = "tamperedUser" + ":" + parts[1] + ":" + parts[2]; // Changed userId
        String tamperedToken = java.util.Base64.getEncoder().encodeToString(tamperedDecoded.getBytes(StandardCharsets.UTF_8));

        InvalidTokenException exception = assertThrows(InvalidTokenException.class, () -> tokenService.validateToken(tamperedToken));
        assertEquals("Token signature is invalid.", exception.getMessage());
    }

    @Test
    void validateToken_expiredToken_throwsInvalidTokenException() {
        // 1. Generate a token using the standard tokenService (which has a positive expiry).
        String originalToken = tokenService.generateToken("expiryUser");

        // 2. Decode the token to manipulate its parts.
        String decodedOriginal = new String(java.util.Base64.getDecoder().decode(originalToken), StandardCharsets.UTF_8);
        String[] parts = decodedOriginal.split(":");
        String userId = parts[0];
        // String originalExpiryStr = parts[1]; // Not directly used, we set a new past time.
        // String originalSignature = parts[2]; // Not used, we recalculate signature.

        // 3. Create a timestamp representing a time well in the past.
        // Ensure it's significantly past to avoid race conditions with System.currentTimeMillis().
        long pastTimeMillis = System.currentTimeMillis() - (TEST_EXPIRY_MINUTES * 60 * 1000 * 2); // e.g., twice the configured expiry ago

        // 4. Construct the data part of the token with this past expiry time.
        String dataToSignWithPastExpiry = userId + ":" + pastTimeMillis;

        // 5. Recalculate the signature for this modified data (userId + past_timestamp).
        // Use the 'calculateSignature' method from our 'tokenService' instance.
        String signatureForPastExpiry = tokenService.calculateSignature(dataToSignWithPastExpiry);

        // 6. Re-assemble the token string with the past expiry and the newly calculated signature.
        String expiredTokenPayload = dataToSignWithPastExpiry + ":" + signatureForPastExpiry;
        String expiredToken = java.util.Base64.getEncoder().encodeToString(expiredTokenPayload.getBytes(StandardCharsets.UTF_8));

        // 7. Attempt to validate this manipulated, expired token.
        InvalidTokenException exception = assertThrows(InvalidTokenException.class, () -> {
            tokenService.validateToken(expiredToken);
        });
        assertEquals("Token has expired.", exception.getMessage());
    }
}
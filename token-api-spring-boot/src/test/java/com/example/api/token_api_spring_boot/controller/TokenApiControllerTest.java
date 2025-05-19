package com.example.api.token_api_spring_boot.controller;

import com.example.api.token_api_spring_boot.dto.TokenGenerationRequest;
import com.example.api.token_api_spring_boot.dto.TokenValidationRequest;
import com.example.secureapp.InvalidTokenException;
import com.example.secureapp.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TokenApiController.class) // Tests only the controller layer
public class TokenApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper; // For converting objects to JSON

    @MockBean // Mocks the TokenService dependency
    private TokenService tokenService;
    // MeterRegistry is automatically mocked by Spring Boot in @WebMvcTest if it's a constructor arg in controller

    @Test
    void generateToken_validUserId_shouldReturnToken() throws Exception {
        String userId = "testUser";
        String mockToken = "mockGeneratedTokenValue";
        TokenGenerationRequest request = new TokenGenerationRequest();
        request.setUserId(userId);

        given(tokenService.generateToken(userId)).willReturn(mockToken);

        mockMvc.perform(post("/api/token/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(mockToken));
    }

    @Test
    void generateToken_blankUserId_shouldReturnBadRequest() throws Exception {
        TokenGenerationRequest request = new TokenGenerationRequest(); 
        request.setUserId(" "); // Blank user Id

        // @Valid annotation and GlobalExceptionHandler will handle this, no need to mock TokenService behavior
        mockMvc.perform(post("/api/token/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("validation_error"))
                .andExpect(jsonPath("$.reason").value("userId: User ID cannot be blank"));
    }

    @Test
    void generateToken_nullUserIdInRequest_shouldReturnBadRequestFromValidation() throws Exception {
        TokenGenerationRequest requestWithNullUserId = new TokenGenerationRequest();
        requestWithNullUserId.setUserId(null);

        mockMvc.perform(post("/api/token/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestWithNullUserId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("validation_error"))
                .andExpect(jsonPath("$.reason").value("userId: User ID cannot be blank"));
    }


    @Test
    void validateToken_validToken_shouldReturnValidStatus() throws Exception {
        String validToken = "aValidTokenString";
        TokenValidationRequest request = new TokenValidationRequest();
        request.setToken(validToken);
        // For validateToken, if it's valid, it returns true (or doesn't throw an exception)
        given(tokenService.validateToken(validToken)).willReturn(true); // Or simply don't mock if no exception

        mockMvc.perform(post("/api/token/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("valid"));
    }

    @Test
    void validateToken_invalidToken_shouldReturnUnauthorized() throws Exception {
        String invalidToken = "anInvalidTokenString";
        String errorMessage = "Token signature is invalid.";
        TokenValidationRequest request = new TokenValidationRequest();
        request.setToken(invalidToken);

        doThrow(new InvalidTokenException(errorMessage)).when(tokenService).validateToken(invalidToken);

        mockMvc.perform(post("/api/token/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized()) // HTTP 401
                .andExpect(jsonPath("$.status").value("invalid"))
                .andExpect(jsonPath("$.reason").value(errorMessage));
    }

    @Test
    void validateToken_blankToken_shouldReturnBadRequest() throws Exception {
        TokenValidationRequest request = new TokenValidationRequest(); 
        request.setToken(" ");// Blank token

        mockMvc.perform(post("/api/token/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("validation_error"))
                .andExpect(jsonPath("$.reason").value("token: Token cannot be blank"));
    }
}
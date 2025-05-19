package com.example.api.token_api_spring_boot.dto;

import jakarta.validation.constraints.NotBlank;

public class TokenGenerationRequest {
    @NotBlank(message = "User ID cannot be blank")
    private String userId;

    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
}
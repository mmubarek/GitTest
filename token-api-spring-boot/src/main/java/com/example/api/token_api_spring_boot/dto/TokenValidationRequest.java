package com.example.api.token_api_spring_boot.dto;

import jakarta.validation.constraints.NotBlank;

public class TokenValidationRequest {
    @NotBlank(message = "Token cannot be blank")
    private String token;

    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }
}
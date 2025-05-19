package com.example.api.token_api_spring_boot.dto;

public class TokenResponse {
    private String token;

    public TokenResponse(String token) {
        this.token = token;
    }
    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }
}
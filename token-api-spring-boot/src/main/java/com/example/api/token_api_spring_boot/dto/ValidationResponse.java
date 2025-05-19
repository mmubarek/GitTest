package com.example.api.token_api_spring_boot.dto;

public class ValidationResponse {
    private String status; // e.g., "valid", "invalid"
    private String reason; // Optional: reason for invalidation

    public ValidationResponse(String status) {
        this.status = status;
    }

    public ValidationResponse(String status, String reason) {
        this.status = status;
        this.reason = reason;
    }
    // Getters and setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
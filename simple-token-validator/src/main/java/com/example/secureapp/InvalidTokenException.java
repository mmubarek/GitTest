// src/main/java/com/example/secureapp/InvalidTokenException.java
package com.example.secureapp;

public class InvalidTokenException extends Exception {
    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
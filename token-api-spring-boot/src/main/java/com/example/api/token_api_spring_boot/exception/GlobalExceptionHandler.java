package com.example.api.token_api_spring_boot.exception;

import com.example.api.token_api_spring_boot.dto.ValidationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation error for request [{}]: {}", request.getDescription(false) , errorMessage);
        return new ResponseEntity<>(new ValidationResponse("validation_error", errorMessage), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ValidationResponse> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        log.warn("Illegal argument for request [{}]: {}",request.getDescription(false), ex.getMessage());
        return new ResponseEntity<>(new ValidationResponse("bad_request", ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    // Catch-all for other unhandled exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ValidationResponse> handleAllOtherExceptions(Exception ex, WebRequest request) {
        log.error("An unexpected error occurred for request [{}]:", request.getDescription(false), ex);
        return new ResponseEntity<>(new ValidationResponse("internal_error", "An unexpected internal error occurred. Please try again later."), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
package com.example.bookshop.exception;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.bookshop.dto.ErrorResponse;
import com.example.bookshop.dto.ErrorResponse.FieldViolation;

/**
 * Catches exceptions thrown from any controller and turns them into
 * a consistent JSON error response, so controllers do not need try/catch.
 *
 * Flow: Controller throws -> Spring finds matching @ExceptionHandler -> JSON error returned
 *
 * | Exception                       | Status | Description                   |
 * |---------------------------------|--------|-------------------------------|
 * | ApiException                    | varies | Uses status and message set when thrown |
 * | MethodArgumentNotValidException | 400    | @Valid failed; returns field errors    |
 * | Exception (anything else)       | 500    | Logs full error; returns safe message  |
 *
 * | Key                   | Explanation                                                  |
 * |-----------------------|--------------------------------------------------------------|
 * | @RestControllerAdvice | Applies handlers to all controllers; returns JSON            |
 * | @ExceptionHandler     | Maps an exception type to the method that handles it         |
 * | ResponseEntity        | Sets both the HTTP status and the response body              |
 * | Logger (SLF4J)        | Records unexpected errors on the server for debugging        |
 *
 * Spring selects the most specific matching handler, so the
 * Exception.class handler runs only when no more specific handler matches.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(ErrorResponse.of(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .toList();

        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("Validation failed", violations));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("Something went wrong. Please try again later."));
    }
}

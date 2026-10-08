package com.example.bookshop.exception;

import org.springframework.http.HttpStatus;

/**
 * Custom exception that carries an HTTP status along with the error message.
 * Throw it from services; GlobalExceptionHandler converts it into a JSON response.
 *
 * Usage: throw ApiException.notFound("Book not found with id " + id);
 *
 * | Factory method | Status | When to use                                        |
 * |----------------|--------|----------------------------------------------------|
 * | badRequest()   | 400    | Input is invalid beyond simple field validation    |
 * | unauthorized()| 401    | User is not logged in or credentials are invalid  |
 * | forbidden()    | 403    | User is logged in but not allowed to do this       |
 * | notFound()     | 404    | Requested resource does not exist                 |
 * | conflict()     | 409    | Clashes with existing data                        |
 *
 * | Key              | Explanation                                                   |
 * |------------------|---------------------------------------------------------------|
 * | RuntimeException | Unchecked; methods do not need a throws declaration           |
 * | super(message)   | Stores the message, later available through getMessage()     |
 * | final status     | Status is fixed once the exception is created                 |
 * | static factories | Readable shortcuts for creating status-specific exceptions   |
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, message);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, message);
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }
}

# Tutorial 09 — Error handling

Return useful, consistent errors instead of exposing framework error pages.

Files for this stage:

- New: `src/main/java/com/example/bookshop/exception/ApiException.java`
- New: `src/main/java/com/example/bookshop/dto/ErrorResponse.java`
- New: `src/main/java/com/example/bookshop/exception/GlobalExceptionHandler.java`
- Updated: `src/main/java/com/example/bookshop/service/BookService.java`

---

## 1. Create an application exception

Create `ApiException.java`. It carries the HTTP status and a message for an expected problem, such as a book id that does not exist:

```java
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
```

It extends `RuntimeException`, so a service can throw it without adding a `throws` declaration to every method.

## 2. Throw it from the service

In `BookService`, replace the temporary exception from tutorial 6 with `ApiException.notFound(...)`:

```java
public Book getBookById(Long id) {
    return bookRepository.findById(id)
            .orElseThrow(() -> ApiException.notFound("Book with id " + id + " not found"));
}
```

The service describes what went wrong. The global handler will translate it into an HTTP response.

## 3. Define one error response shape

Create `ErrorResponse.java`. `FieldViolation` identifies a request field that failed validation. `@JsonInclude` omits `errors` when there are no field details.

```java
package com.example.bookshop.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(boolean success, String message, List<FieldViolation> errors) {

    public record FieldViolation(String field, String message) {
    }

    public static ErrorResponse of(String message) {
        return new ErrorResponse(false, message, null);
    }

    public static ErrorResponse of(String message, List<FieldViolation> errors) {
        return new ErrorResponse(false, message, errors);
    }
}
```

## 4. Handle errors in one advice class

Create `GlobalExceptionHandler.java`. The specific handlers turn known errors into their appropriate status codes. The final handler logs unexpected exceptions on the server and returns a generic message to the client instead of exposing a stack trace.

```java
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
```

This advice handles exceptions raised while processing controller requests. Errors raised earlier in the Spring Security filter chain need their own handling and are covered in tutorial 16.

## 5. Check the responses

Request a book id that does not exist:

```http
GET /api/v1/books/999
```

Response: HTTP `404 Not Found`.

Send invalid values from tutorial 8:

```http
POST /api/v1/books
Content-Type: application/json

{"title":"","author":"","price":-5}
```

Response: HTTP `400 Bad Request` with field-specific messages:

```json
{
  "success": false,
  "message": "Validation failed",
  "errors": [
    { "field": "title", "message": "title is required" },
    { "field": "author", "message": "author is required" },
    { "field": "price", "message": "price must be greater than 0" }
  ]
}
```

Next: [**Tutorial 10 — Relationships and queries**](tutorial-10%20%28Relationships%20and%20queries%29.md)

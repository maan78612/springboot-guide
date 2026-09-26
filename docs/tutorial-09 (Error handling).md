# Tutorial 09 — Error handling

Handle API failures in one place.

Files for this stage:

- New: `src/main/java/com/example/bookshop/exception/ApiException.java`
- New: `src/main/java/com/example/bookshop/exception/GlobalExceptionHandler.java`
- New: `src/main/java/com/example/bookshop/dto/ErrorResponse.java`
- Updated: `src/main/java/com/example/bookshop/service/BookService.java`
- Updated: `src/main/java/com/example/bookshop/controller/BookController.java`

---

## 1. Throw domain errors from the service

```java
throw ApiException.notFound("Book with id " + id + " not found");
```

This replaces scattered `Optional` checks and empty 404 responses.

## 2. Catch everything in one advice class

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(ErrorResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.validation(ex.getBindingResult()));
    }
}
```

One handler keeps the client-facing error format consistent.

## 3. Error response format

```java
public record ErrorResponse(boolean success, String message, List<FieldErrorItem> errors) {
    public static ErrorResponse error(String message) {
        return new ErrorResponse(false, message, null);
    }
}
```

Sample response:

```json
{
  "success": false,
  "message": "Validation failed",
  "errors": [{ "field": "title", "message": "title is required" }]
}
```

## 4. Example failures

```json
GET /api/v1/books/999
{"success":false,"message":"Book with id 999 not found"}
```

```json
POST /api/v1/books
{"title":"","author":"","price":-5}
{"success":false,"message":"Validation failed","errors":[...]}
```

Next: [**Tutorial 10 — Relationships and queries**](tutorial-10%20%28Relationships%20and%20queries%29.md)

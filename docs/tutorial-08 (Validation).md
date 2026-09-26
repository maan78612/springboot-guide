# Tutorial 08 — Validation

Reject bad input before it reaches the service.

Files for this stage:

- Updated: `src/main/java/com/example/bookshop/dto/BookRequest.java`
- Updated: `src/main/java/com/example/bookshop/controller/BookController.java`

---

## 1. Add constraints to the request DTO

```java
public record BookRequest(
        @NotBlank(message = "title is required")
        @Size(max = 200, message = "title must be at most 200 characters")
        String title,

        @NotBlank(message = "author is required")
        String author,

        @NotNull(message = "price is required")
        @Positive(message = "price must be greater than 0")
        @Digits(integer = 8, fraction = 2, message = "price must have at most 2 decimal places")
        BigDecimal price) {
}
```

These checks stop empty titles, missing authors, and negative or malformed prices.

## 2. Use @Valid on the controller

```java
@PostMapping
public ResponseEntity<ApiResponse<BookResponse>> createBook(@Valid @RequestBody BookRequest request) {
    Book saved = bookService.createBook(request);
    return ResponseEntity.created(URI.create("/api/v1/books/" + saved.getId()))
            .body(ApiResponse.ok("Book created", BookResponse.from(saved)));
}
```

Spring validates the JSON body before entering the method.

## 3. Example failure

```json
POST /api/v1/books
{"title":"","author":"","price":-5}
```

Returns a 400 instead of creating a broken row.

Next: [**Tutorial 09 — Error handling**](tutorial-09%20%28Error%20handling%29.md)

# Tutorial 08 — Validation

Reject bad input before it reaches the service.

Files for this stage:

- Updated: `pom.xml`
- Updated: `src/main/java/com/example/bookshop/dto/BookRequest.java`
- Updated: `src/main/java/com/example/bookshop/controller/BookController.java`

---

## 1. Add the validation dependency

Add this inside the `<dependencies>` section of `pom.xml`:

```xml
<dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

This provides the validation annotations and runtime support. If your IDE says `NotBlank` or another constraint cannot be resolved, check that this dependency is present and Maven has reloaded the project.

## 2. Add constraints to the request DTO

```java
package com.example.bookshop.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record BookRequest(
        // Rejects null, empty, or whitespace-only titles.
        @NotBlank(message = "title is required")
        // Limits the title to 200 characters.
        @Size(max = 200, message = "title must be at most 200 characters")
        String title,

        // Rejects null, empty, or whitespace-only author names.
        @NotBlank(message = "author is required")
        String author,

        // Rejects a missing/null price.
        @NotNull(message = "price is required")
        // Requires the price to be greater than zero.
        @Positive(message = "price must be greater than 0")
        // Allows up to 8 digits before and 2 digits after the decimal point.
        @Digits(integer = 8, fraction = 2, message = "price must have at most 2 decimal places")
        BigDecimal price) {
}
```

Use `jakarta.validation.constraints.NotBlank`, not `org.hibernate.validator.constraints.NotBlank`. Each annotation checks a rule when Spring validates the request.

## 3. Run validation for request bodies

Import `jakarta.validation.Valid` in `BookController.java`. Add `@Valid` before `@RequestBody` on both create and update, so Spring checks the DTO before calling the service:

```java
import jakarta.validation.Valid;

// In BookController; keep the existing controller and other imports.
@PostMapping
public ResponseEntity<ApiResponse<BookResponse>> createBook(
                // @Valid runs the constraints declared on BookRequest before this method proceeds.
                @Valid @RequestBody BookRequest request) {
    Book saved = bookService.createBook(request);
        // ResponseEntity lets create return 201 Created and a Location header.
    return ResponseEntity.created(URI.create("/api/v1/books/" + saved.getId()))
            .body(ApiResponse.ok("Book created", BookResponse.from(saved)));
}

@PutMapping("/{id}")
public ApiResponse<BookResponse> updateBook(@PathVariable Long id,
        @Valid @RequestBody BookRequest request) {
    return ApiResponse.ok("Book updated",
            BookResponse.from(bookService.updateBook(id, request)));
}
```

`@Valid` triggers the annotations on `BookRequest`. Without it, those annotations are not checked for these controller requests. `ResponseEntity` is used for create because it returns `201 Created` and a `Location` header; the update route uses the default `200 OK`.

## 4. Try invalid input

```bash
curl -i -X POST http://localhost:8080/api/v1/books \
        -H 'Content-Type: application/json' \
        -d '{"title":"","author":"","price":-5}'
```

The request is rejected with HTTP `400 Bad Request` instead of creating a row. This example assumes you have not yet added authentication in tutorial 15. After tutorial 15, send a valid bearer token too; otherwise security returns `401 Unauthorized` before validation runs. Tutorial 9 adds the app's consistent JSON error response for validation failures.

Next: [**Tutorial 09 — Error handling**](tutorial-09%20%28Error%20handling%29.md)

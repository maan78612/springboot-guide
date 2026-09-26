# Tutorial 07 — DTOs and the response envelope

Keep entity internals out of the API.

Files for this stage:

- New: `src/main/java/com/example/bookshop/dto/ApiResponse.java`
- New: `src/main/java/com/example/bookshop/dto/BookRequest.java`
- New: `src/main/java/com/example/bookshop/dto/BookResponse.java`
- Updated: `src/main/java/com/example/bookshop/model/Book.java`
- Updated: `src/main/java/com/example/bookshop/controller/BookController.java`

---

## 1. Add the response envelope

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, String message, T data, Object meta) {

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static <T> ApiResponse<T> ok(String message, T data, Object meta) {
        return new ApiResponse<>(true, message, data, meta);
    }
}
```

Every response now has the same shape:

```json
{ "success": true, "message": "Book fetched", "data": { ... } }
```

## 2. Add request and response DTOs

```java
public record BookRequest(
        String title,
        String author,
        BigDecimal price) {
}
```

```java
public record BookResponse(Long id, String title, String author, BigDecimal price) {
    public static BookResponse from(Book book) {
        return new BookResponse(book.getId(), book.getTitle(),
                book.getAuthor().getName(), book.getPrice());
    }
}
```

This keeps internal fields like `costPrice` out of the API.

## 3. Controller returns DTOs, not entities

```java
@GetMapping("/{id}")
public ApiResponse<BookResponse> getBookById(@PathVariable Long id) {
    return ApiResponse.ok("Book fetched", BookResponse.from(bookService.getBookById(id)));
}
```

```java
@PostMapping
public ResponseEntity<ApiResponse<BookResponse>> createBook(@Valid @RequestBody BookRequest request) {
    Book saved = bookService.createBook(request);
    return ResponseEntity.created(URI.create("/api/v1/books/" + saved.getId()))
            .body(ApiResponse.ok("Book created", BookResponse.from(saved)));
}
```

## 4. Why this matters

This prevents API leaks like:

```json
{ "id": 1, "title": "Effective Java", "price": 54.99, "costPrice": 31.0 }
```

`costPrice` is an internal database concern, not a public API field.

Next: [**Tutorial 08 — Validation**](tutorial-08%20%28Validation%29.md)

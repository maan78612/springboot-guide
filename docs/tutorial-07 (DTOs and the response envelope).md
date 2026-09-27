# Tutorial 07 — DTOs and the response envelope

Keep database entities separate from the JSON sent to and received from clients.

Files for this stage:

- New: `src/main/java/com/example/bookshop/dto/ApiResponse.java`
- New: `src/main/java/com/example/bookshop/dto/BookRequest.java`
- New: `src/main/java/com/example/bookshop/dto/BookResponse.java`
- Updated: `src/main/java/com/example/bookshop/model/Book.java`
- Updated: `src/main/java/com/example/bookshop/service/BookService.java`
- Updated: `src/main/java/com/example/bookshop/controller/BookController.java`

At this stage, `Book.author` is still a string, as in tutorial 6. Tutorial 10 changes it into an author relationship. Validation annotations are added in tutorial 8.

---

## 1. Add an internal field to `Book`

Add `costPrice` to the `Book` entity. The shop needs this value internally, but it should not be part of the public API.

```java
@Column(precision = 10, scale = 2)
private BigDecimal costPrice;

public BigDecimal getCostPrice() {
    return costPrice;
}

public void setCostPrice(BigDecimal costPrice) {
    this.costPrice = costPrice;
}
```

Keep the existing fields, imports, constructors, and methods. If your database table is managed by Flyway, add the matching column through a new migration rather than changing an already-applied migration.

## 2. Create the response envelope

Create `ApiResponse.java`. The `data` field can hold one result or a list; `meta` can hold optional extra information such as pagination later.

```java
package com.example.bookshop.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

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

For example, the response becomes:

```json
{"success":true,"message":"Book fetched","data":{"id":1,"title":"Effective Java","author":"Joshua Bloch","price":54.99}}
```

## 3. Create the request and response DTOs

Create `BookRequest.java`. The client sends only fields it is allowed to set: no database id and no internal `costPrice`.

```java
package com.example.bookshop.dto;

import java.math.BigDecimal;

public record BookRequest(String title, String author, BigDecimal price) {
}
```

Create `BookResponse.java`. Its `from` method chooses exactly which entity fields are returned:

```java
package com.example.bookshop.dto;

import java.math.BigDecimal;

import com.example.bookshop.model.Book;

public record BookResponse(Long id, String title, String author, BigDecimal price) {

    public static BookResponse from(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getPrice());
    }
}
```

## 4. Map requests in the service

Update the create and update methods in `BookService`. Convert the incoming request into entity fields, then save the entity through the repository:

```java
public Book createBook(BookRequest request) {
    Book book = new Book(request.title(), request.author(), request.price());
    return bookRepository.save(book);
}

public Book updateBook(Long id, BookRequest changes) {
    Book book = getBookById(id);
    book.setTitle(changes.title());
    book.setAuthor(changes.author());
    book.setPrice(changes.price());
    return bookRepository.save(book);
}
```

Keep the existing `getAllBooks`, `getBookById`, and `deleteBook` service methods from tutorial 6.

## 5. Return DTOs from every controller route

Replace the tutorial 6 controller with this version. Each route converts entities to `BookResponse`; the create route keeps its `201 Created` status and `Location` header.

```java
package com.example.bookshop.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bookshop.dto.ApiResponse;
import com.example.bookshop.dto.BookRequest;
import com.example.bookshop.dto.BookResponse;
import com.example.bookshop.model.Book;
import com.example.bookshop.service.BookService;

@RestController
@RequestMapping("/api/v1/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public ApiResponse<List<BookResponse>> getAllBooks() {
        List<BookResponse> books = bookService.getAllBooks().stream()
                .map(BookResponse::from)
                .toList();
        return ApiResponse.ok("Books fetched", books);
    }

    @GetMapping("/{id}")
    public ApiResponse<BookResponse> getBookById(@PathVariable Long id) {
        return ApiResponse.ok("Book fetched", BookResponse.from(bookService.getBookById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BookResponse>> createBook(@RequestBody BookRequest request) {
        Book saved = bookService.createBook(request);
        return ResponseEntity.created(URI.create("/api/v1/books/" + saved.getId()))
                .body(ApiResponse.ok("Book created", BookResponse.from(saved)));
    }

    @PutMapping("/{id}")
    public ApiResponse<BookResponse> updateBook(@PathVariable Long id,
            @RequestBody BookRequest request) {
        return ApiResponse.ok("Book updated",
                BookResponse.from(bookService.updateBook(id, request)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ApiResponse.ok("Book deleted", null);
    }
}
```

`@RequestBody` converts JSON into `BookRequest`. The DTO does not have an `id` or `costPrice`, so clients cannot set those fields through these endpoints. Tutorial 8 adds `@Valid` and field constraints; tutorial 9 replaces the temporary not-found exception with the standard error response.

## 6. Why use DTOs?

Returning the entity directly could expose internal fields such as `costPrice`. A response DTO is an allow-list: only the fields declared in `BookResponse` are sent to the client.

Next: [**Tutorial 08 — Validation**](tutorial-08%20%28Validation%29.md)
# Tutorial 06 — Full CRUD API

Replace the read-only books endpoint with create, read, update, and delete operations.

Files for this stage:

- Updated: `src/main/java/com/example/bookshop/model/Book.java`
- Updated: `src/main/java/com/example/bookshop/service/BookService.java`
- Updated: `src/main/java/com/example/bookshop/controller/BookController.java`

This version returns `Book` directly to keep the first CRUD example small. Tutorial 7 will replace that with request/response DTOs.

---

## 1. Add setters to `Book`

The update operation loads the saved book and changes its fields. Add these setters to the `Book` class from tutorial 5:

```java
public void setTitle(String title) {
    this.title = title;
}

public void setAuthor(String author) {
    this.author = author;
}

public void setPrice(BigDecimal price) {
    this.price = price;
}
```

Keep the getters, constructors, and JPA annotations already in the class.

## 2. Implement the CRUD operations in `BookService`

Replace the tutorial 3 service with this version:

```java
package com.example.bookshop.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.bookshop.model.Book;
import com.example.bookshop.repository.BookRepository;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public Book getBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found: " + id));
    }

    public Book createBook(Book book) {
        return bookRepository.save(book);
    }

    public Book updateBook(Long id, Book changes) {
        Book book = getBookById(id);
        book.setTitle(changes.getTitle());
        book.setAuthor(changes.getAuthor());
        book.setPrice(changes.getPrice());
        return bookRepository.save(book);
    }

    public void deleteBook(Long id) {
        Book book = getBookById(id);
        bookRepository.delete(book);
    }
}
```

`findAll`, `findById`, `save`, and `delete` are supplied by `JpaRepository`. The service loads the existing book before updating or deleting it. Tutorial 9 replaces the temporary `RuntimeException` with the app's standard not-found error handling.

## 3. Add all five endpoints to `BookController`

Replace the tutorial 3 controller with:

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
    public List<Book> getAllBooks() {
        return bookService.getAllBooks();
    }

    @GetMapping("/{id}")
    public Book getBookById(@PathVariable Long id) {
        return bookService.getBookById(id);
    }

    @PostMapping
    public ResponseEntity<Book> createBook(@RequestBody Book book) {
        Book saved = bookService.createBook(book);
        return ResponseEntity.created(URI.create("/api/v1/books/" + saved.getId()))
                .body(saved);
    }

    @PutMapping("/{id}")
    public Book updateBook(@PathVariable Long id, @RequestBody Book changes) {
        return bookService.updateBook(id, changes);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
}
```

`@PathVariable` reads the `id` from the URL. `@RequestBody` converts incoming JSON into a `Book`. The create endpoint returns `201 Created` with a `Location` header; delete returns `204 No Content`.

## 4. Try the endpoints

Start the app:

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

Create a book:

```bash
curl -i -X POST http://localhost:8080/api/v1/books \
  -H 'Content-Type: application/json' \
  -d '{"title":"Clean Code","author":"Robert C. Martin","price":42.50}'
```

Then use the returned `id` to try:

```bash
curl http://localhost:8080/api/v1/books
curl http://localhost:8080/api/v1/books/1

curl -X PUT http://localhost:8080/api/v1/books/1 \
  -H 'Content-Type: application/json' \
  -d '{"title":"Clean Code, Updated","author":"Robert C. Martin","price":45.00}'

curl -i -X DELETE http://localhost:8080/api/v1/books/1
```

The project adds authentication later, so once tutorial 15 is applied, write requests require a bearer token.

## 5. Remember

- Controller: maps HTTP requests to service calls and chooses the HTTP response.
- Service: performs the operation using the repository.
- Repository: `JpaRepository` reads and writes database rows.
- Tutorial 7 replaces direct entity input/output with DTOs; tutorial 9 adds consistent error handling.

Next: [**Tutorial 07 — DTOs and the response envelope**](tutorial-07%20%28DTOs%20and%20the%20response%20envelope%29.md)

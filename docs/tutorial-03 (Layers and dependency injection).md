# Tutorial 03 — Layers and dependency injection

Move the book list out of the controller and into proper layers.

Files for this stage:

- `src/main/java/com/example/bookshop/repository/BookRepository.java`
- `src/main/java/com/example/bookshop/service/BookService.java`
- `src/main/java/com/example/bookshop/controller/BookController.java`

---

## 1. Separate the responsibilities

The controller should only handle HTTP.
The service should handle business logic.
The repository should handle data access.

Flow:

```text
controller -> service -> repository
```

This keeps the app easier to expand later.

## 2. Simple repository example

At this stage, the repository can be a simple interface.

```java
package com.example.bookshop.repository;

import java.util.List;

import com.example.bookshop.model.Book;

public interface BookRepository {
    List<Book> getAllBooks();
}
```

Later, this becomes a full JPA repository with real database methods.

## 3. Service with constructor injection

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
        return bookRepository.getAllBooks();
    }
}
```

This is the key idea:

- the service declares what it needs
- Spring injects it automatically

## 4. Controller delegates to the service

```java
package com.example.bookshop.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
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
}
```

Now the controller is thin and focused on HTTP only.

## 5. Why constructor injection is preferred

This pattern is used:

```java
private final BookRepository bookRepository;

public BookService(BookRepository bookRepository) {
    this.bookRepository = bookRepository;
}
```

Benefits:

- clear dependencies
- easier to test
- no manual `new` calls for app layers
- Spring wires the object automatically

## 6. Run it

```bash
./mvnw spring-boot:run
```

Then call:

```bash
curl http://localhost:8080/api/v1/books
```

The response should still be the same as in tutorial 02.

## 7. Common error

If you see:

```text
required a bean of type 'BookRepository' that could not be found
```

then usually:

- the class is missing a Spring annotation
- the class is outside the scanned package
- the dependency type does not match the bean

## 8. Goal for this tutorial

By the end of this tutorial, you should understand:

- why controller/service/repository are separated
- what dependency injection means
- why constructor injection is used
- how Spring wires beans together

Next: [**Tutorial 04 — Configuration**](tutorial-04%20%28Configuration%29.md)

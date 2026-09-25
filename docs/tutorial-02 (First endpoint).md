# Tutorial 02 — First endpoint

Create your first Spring Boot endpoint and return JSON.

Files for this stage:
- `src/main/java/com/example/bookshop/model/Book.java`
- `src/main/java/com/example/bookshop/controller/BookController.java`

---

## 1. Create the model

`Book` is a plain Java class. It has fields and public getters so Jackson can convert it to JSON.

```java
package com.example.bookshop.model;

import java.math.BigDecimal;

public class Book {

    private Long id;
    private String title;
    private String author;
    private BigDecimal price;

    public Book(Long id, String title, String author, BigDecimal price) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.price = price;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
```

Important:
- use `BigDecimal` for money
- keep the getters public, otherwise Jackson will not serialize the fields properly

## 2. Create the controller

```java
package com.example.bookshop.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bookshop.model.Book;

@RestController
@RequestMapping("/api/v1/books")
public class BookController {

    @GetMapping
    public List<Book> getAllBooks() {
        return List.of(
                new Book(1L, "Effective Java", "Joshua Bloch", new BigDecimal("54.99")),
                new Book(2L, "Clean Code", "Robert C. Martin", new BigDecimal("42.50"))
        );
    }
}
```

What each annotation means:
- `@RestController` → this class handles HTTP and returns JSON
- `@RequestMapping("/api/v1/books")` → base URL for this controller
- `@GetMapping` → this method handles GET requests

## 3. Run the app

```bash
./mvnw spring-boot:run
```

Then call:

```bash
curl http://localhost:8080/api/v1/books
```

You should get:

```json
[
  {"id":1,"title":"Effective Java","author":"Joshua Bloch","price":54.99},
  {"id":2,"title":"Clean Code","author":"Robert C. Martin","price":42.50}
]
```

## 4. Why it works

Spring uses Jackson to convert Java objects to JSON.
It looks at the public getters and turns them into fields in the JSON response.

Example:
- `getTitle()` → `"title"`
- `getPrice()` → `"price"`

## 5. Common mistakes

- wrong URL: `/books` instead of `/api/v1/books`
- missing public getter
- using `double` instead of `BigDecimal` for price

## 6. Goal for this tutorial

By the end of this tutorial, you should understand:
- how a controller maps a URL
- how a Java object becomes JSON
- how `@RestController`, `@RequestMapping`, and `@GetMapping` work together

Next: [**Tutorial 03 — Layers and dependency injection.**](tutorial-03%20%28Layers%20and%20dependency%20injection%29.md)
- `@RestController` means "return values are the JSON body".
- Jackson converts objects to JSON using public getters. No getters →
  silent `{}`.
- Money is `BigDecimal`. Always.
- The hardcoded list inside the controller is deliberately wrong —
  a controller should hold no data. Next tutorial fixes exactly that.

Next: [**Tutorial 03 — Layers and dependency injection.**](tutorial-03%20%28Layers%20and%20dependency%20injection%29.md)

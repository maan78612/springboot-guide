# Tutorial 13 — Testing

Test at the right level for the right question. Learn how to structure tests across the testing pyramid using unit tests with Mockito, controller slice tests with `@WebMvcTest` + `MockMvc`, and full integration tests with `@SpringBootTest`.

Files for this stage:

- New: `src/test/java/com/example/bookshop/service/BookServiceTest.java` (Unit tests)
- New: `src/test/java/com/example/bookshop/controller/BookControllerTest.java` (Controller slice tests)
- New: `src/test/java/com/example/bookshop/BookshopApplicationTests.java` (Full application integration tests)

---

## The Testing Pyramid

One kind of test cannot check everything efficiently. Whole-app tests are thorough but slow; unit tests are fast but cannot see HTTP serialization or database interactions. A healthy test suite follows the testing pyramid:

| Level | Starts | Speed | What to Test Here |
| :--- | :--- | :--- | :--- |
| **Unit Tests** (Mockito) | Plain Java objects only; no Spring context | ~Milliseconds | Business logic: discount calculations, duplicate checks, price clamping |
| **Slice Tests** (`@WebMvcTest`) | Controller + Jackson JSON + Validation + GlobalExceptionHandler | ~Seconds | HTTP status codes, JSON response envelope, validation annotations, error translation |
| **Full Stack Tests** (`@SpringBootTest`) | Complete Spring context + All Beans + H2 Database + Flyway seeds | ~Several seconds | End-to-end integration: "does the whole application hang together" |

Run the test suite with:
```bash
./mvnw test
```

---

## 1. Unit tests for business rules

Unit tests check one class in complete isolation. Dependencies are replaced with **mocks** — fake objects created by Mockito that return scripted responses and record how they were called.

Because `BookService` uses constructor injection, we can instantiate it directly with `new` without starting the Spring container:

```java
package com.example.bookshop.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.example.bookshop.config.BookshopProperties;
import com.example.bookshop.exception.ApiException;
import com.example.bookshop.model.Author;
import com.example.bookshop.model.Book;
import com.example.bookshop.repository.AuthorRepository;
import com.example.bookshop.repository.BookRepository;
import com.example.bookshop.repository.GenreRepository;

/**
 * | Key                  | Why we use it                                 |
 * |----------------------|-----------------------------------------------|
 * | @ExtendWith          | Connects JUnit 5 to Mockito                   |
 * | @Mock                | Creates a fake repository dependency          |
 * | BookServiceTest      | Tests service behavior without starting Spring|
 */
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock private BookRepository bookRepository;
    @Mock private AuthorRepository authorRepository;
    @Mock private GenreRepository genreRepository;

    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookService = new BookService(bookRepository, authorRepository, genreRepository, new BookshopProperties());
    }

    @Test
    void getBookById_throwsNotFound_whenMissing() {
        when(bookRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getBookById(42L))
                .isInstanceOf(ApiException.class)
                .hasMessage("Book with id 42 not found");
    }

    @Test
    void applyAuthorDiscount_discountsEveryBook() {
        Author author = new Author("Kent Beck");
        Book book = new Book("TDD", author, new BigDecimal("50.00"));
        book.setCostPrice(new BigDecimal("20.00"));
        when(authorRepository.existsById(7L)).thenReturn(true);
        when(bookRepository.findByAuthorId(7L)).thenReturn(List.of(book));

        bookService.applyAuthorDiscount(7L, 10);

        assertThat(book.getPrice()).isEqualTo(new BigDecimal("45.00"));
        verify(bookRepository).save(book);
    }

    @Test
    void applyAuthorDiscount_rejectsPriceBelowCost() {
        Author author = new Author("Kent Beck");
        Book book = new Book("TDD", author, new BigDecimal("50.00"));
        book.setCostPrice(new BigDecimal("49.00"));
        when(authorRepository.existsById(7L)).thenReturn(true);
        when(bookRepository.findByAuthorId(7L)).thenReturn(List.of(book));

        assertThatThrownBy(() -> bookService.applyAuthorDiscount(7L, 10))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("below its cost price");

        verify(bookRepository, never()).save(any());
    }
}
```

These tests focus on logic such as validation, discount checks, and repository calls.

---

## 2. Web layer tests with MockMvc

`@WebMvcTest` starts only the web slice (Spring MVC, Jackson, and `@RestControllerAdvice`), leaving out database repositories and business services. We mock the service with `@MockitoBean`.

> [!NOTE]
> Spring Boot 3.4+ replaces the deprecated `@MockBean` with `@MockitoBean`.

```java
package com.example.bookshop.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.bookshop.exception.ApiException;
import com.example.bookshop.service.BookService;

/**
 * | Key          | Why we use it                                      |
 * |--------------|----------------------------------------------------|
 * | @WebMvcTest  | Starts the MVC slice without the full application |
 * | MockMvc      | Sends simulated HTTP requests to controller routes|
 * | @MockitoBean | Supplies a mock service to the MVC test            |
 */
@WebMvcTest(BookController.class)
class BookControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private BookService bookService;

    @Test
    void createBook_validatesRequiredFields() throws Exception {
        mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void getBookById_returnsNotFound_whenMissing() throws Exception {
        when(bookService.getBookById(99L)).thenThrow(ApiException.notFound("Book with id 99 not found"));

        mockMvc.perform(get("/api/v1/books/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Book with id 99 not found"));
    }
}
```

This checks HTTP status, JSON shape, and validation without booting the whole app.

---

## 3. Full app test with Spring Boot

`@SpringBootTest` loads the complete Spring ApplicationContext with all real beans, configuration properties, and database connections. Combined with `@AutoConfigureMockMvc`, it allows testing full end-to-end HTTP requests against the real application.

```java
package com.example.bookshop;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * | Key                  | Why we use it                                  |
 * |----------------------|------------------------------------------------|
 * | @SpringBootTest      | Loads the full application context             |
 * | @AutoConfigureMockMvc| Provides MockMvc with the full Spring setup    |
 */
@SpringBootTest
@AutoConfigureMockMvc
class BookshopApplicationTests {

    @Autowired private MockMvc mockMvc;

    @Test
    void contextLoads() {
        // Verifies that all beans and configuration properties wire successfully
    }

    @Test
    void fullStack_listBooks_servesSeededData() throws Exception {
        mockMvc.perform(get("/api/v1/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.meta.total").isNumber());
    }
}
```

This validates wiring across real beans, repositories, and config.

---

## 4. Common Pitfalls in Testing

1. **Missing `@MockitoBean` in Slice Tests**:
   If you forget `@MockitoBean private BookService bookService;` in `@WebMvcTest(BookController.class)`, the test fails with:
   `UnsatisfiedDependencyException: No qualifying bean of type BookService available`.
   The web slice does not scan `@Service` beans, so collaborators must be mocked explicitly.
2. **`contextLoads()` is not useless**:
   Even though `contextLoads()` has no assertions in its method body, it is a critical smoke alarm: it fails immediately if any bean cannot be initialized, an entity mapping has a syntax error, or a `@ConfigurationProperties` binding fails.
3. **AssertJ vs JUnit Assertions**:
   Prefer AssertJ's fluent assertions (`assertThat(x).isEqualTo(y)`, `assertThatThrownBy(...)`) for clearer failure messages and readable test logic.

---

## Summary Table

| Annotation | Layer Tested | Starts Context? | Use Case |
| :--- | :--- | :--- | :--- |
| `@ExtendWith(MockitoExtension.class)` | Pure Java / Unit | No | Fast business logic and edge case verification. |
| `@WebMvcTest(Controller.class)` | Spring MVC Slice | Partial (Controller + JSON) | Verifying endpoint routing, validation, and JSON envelopes. |
| `@MockitoBean` | Spring Context Mock | Yes | Replaces a Spring bean with a Mockito mock inside slice tests. |
| `@SpringBootTest` | End-to-End | Full Context | Verifying complete wiring across real database and beans. |

Next: [**Tutorial 14 — Logging**](tutorial-14%20%28Logging%29.md)

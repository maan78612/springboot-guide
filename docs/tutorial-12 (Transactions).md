# Tutorial 12 — Transactions

Transactions ensure database operations succeed together or leave no trace at all. Learn how `@Transactional` works, when rollbacks occur, and how Spring's transaction proxies manage atomicity.

Files for this stage:

- New: `src/main/java/com/example/bookshop/dto/DiscountRequest.java`
- Updated: `src/main/java/com/example/bookshop/service/BookService.java`
- Updated: `src/main/java/com/example/bookshop/controller/AuthorController.java`

---

## 1. Why: Some Operations Must Be All-or-Nothing

> A **transaction** is a set of database operations treated as a single, atomic unit of work: either every operation commits to the database, or the entire set is rolled back as if nothing happened.

The classic banking example is an account transfer (debit Account A, credit Account B). If debiting succeeds but crediting fails, money is permanently lost unless both operations are bound to a transaction.

In our bookshop, our bulk pricing feature demonstrates this exact problem:
**Apply a discount percentage to all books written by a specific author.**

### The Business Rule (Price Floor)
The shop cannot sell a book below what it cost to acquire (`costPrice`). If a discount pushes *any* of an author's books below its `costPrice`, the entire discount operation must be rejected, and **none** of the author's books may be modified.

New endpoint:
```http
POST /api/v1/authors/{id}/discount
Content-Type: application/json

{
  "percent": 46
}
```

---

## 2. Create the `DiscountRequest` DTO

Create `src/main/java/com/example/bookshop/dto/DiscountRequest.java`. This record validates the discount percentage before any business logic executes:

```java
package com.example.bookshop.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Inbound request body for the bulk author discount endpoint.
 *
 * | Key / Constraint | Field   | Explanation                                  |
 * |------------------|---------|----------------------------------------------|
 * | @NotNull         | percent | Requires a discount percentage to be provided|
 * | @Min(1)          | percent | Minimum discount percentage is 1%            |
 * | @Max(90)         | percent | Maximum discount percentage is 90%           |
 */
public record DiscountRequest(
        @NotNull(message = "percent is required")
        @Min(value = 1, message = "percent must be between 1 and 90")
        @Max(value = 90, message = "percent must be between 1 and 90")
        Integer percent) {
}
```

---

## 3. Implement `applyAuthorDiscount` in `BookService`

> [!IMPORTANT]
> **Where does `@Transactional` belong?**
> `@Transactional` belongs on **Service** methods (`@Service`), not on JPA Entities (`Book`) or Repositories.
> - An **Entity** (`Book.java`) is a plain data model representing a table row; putting service methods or `@Transactional` in an entity causes compiler errors and architectural confusion.
> - A **Repository** method only manages single database queries.
> - The **Service** coordinates multiple queries and updates across entities, defining the boundary of a single business operation.

Open `src/main/java/com/example/bookshop/service/BookService.java`. Add these imports:

```java
import java.math.RoundingMode;
import org.springframework.transaction.annotation.Transactional;
```

Add the `applyAuthorDiscount` method inside `BookService`:

```java
    /**
     * Discounts every book of one author within a single transaction.
     * If any book's discounted price drops below its cost price, an ApiException
     * is thrown and all changes are rolled back automatically.
     */
    @Transactional
    public List<Book> applyAuthorDiscount(Long authorId, int percent) {
        if (!authorRepository.existsById(authorId)) {
            throw ApiException.notFound("Author with id " + authorId + " not found");
        }

        BigDecimal factor = BigDecimal.valueOf(100 - percent).movePointLeft(2);
        List<Book> books = bookRepository.findByAuthorId(authorId);

        for (Book book : books) {
            BigDecimal newPrice = book.getPrice().multiply(factor).setScale(2, RoundingMode.HALF_UP);
            if (book.getCostPrice() != null && newPrice.compareTo(book.getCostPrice()) < 0) {
                throw ApiException.conflict("A " + percent + "% discount would push '"
                        + book.getTitle() + "' below its cost price");
            }
            book.setPrice(newPrice);
            bookRepository.save(book);
        }

        return books;
    }
```

### How the Calculation Works
1. `BigDecimal.valueOf(100 - percent).movePointLeft(2)`: For a 10% discount, this creates `0.90`.
2. `book.getPrice().multiply(factor).setScale(2, RoundingMode.HALF_UP)`: Rounds monetary amounts to two decimal places safely.
3. If `newPrice.compareTo(book.getCostPrice()) < 0`, throwing `ApiException.conflict(...)` halts the loop immediately.

---

## 4. Expose the Endpoint in `AuthorController`

Open `src/main/java/com/example/bookshop/controller/AuthorController.java`. Inject `BookService` and add the `POST /{id}/discount` endpoint:

```java
package com.example.bookshop.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bookshop.dto.ApiResponse;
import com.example.bookshop.dto.AuthorResponse;
import com.example.bookshop.dto.BookResponse;
import com.example.bookshop.dto.DiscountRequest;
import com.example.bookshop.service.AuthorService;
import com.example.bookshop.service.BookService;

import jakarta.validation.Valid;

/**
 * Lists authors and applies bulk discounts to an author's books.
 *
 * | Method | Endpoint                      | Status | Description                              |
 * |--------|-------------------------------|--------|------------------------------------------|
 * | GET    | /api/v1/authors               | 200    | Lists authors and their book titles      |
 * | POST   | /api/v1/authors/{id}/discount | 200    | Applies bulk discount to author's books  |
 *
 * | Key             | Why we use it                                      |
 * |-----------------|----------------------------------------------------|
 * | @RestController | Returns the response as JSON                       |
 * | @RequestMapping | Sets the shared `/api/v1/authors` URL prefix       |
 * | @Valid          | Validates DiscountRequest before method execution  |
 * | BookResponse    | Formats discounted books without exposing entities |
 */
@RestController
@RequestMapping("/api/v1/authors")
public class AuthorController {

    private final AuthorService authorService;
    private final BookService bookService;

    public AuthorController(AuthorService authorService, BookService bookService) {
        this.authorService = authorService;
        this.bookService = bookService;
    }

    @GetMapping
    public ApiResponse<List<AuthorResponse>> getAllAuthors() {
        List<AuthorResponse> authors = authorService.getAllAuthors().stream()
                .map(AuthorResponse::from)
                .toList();

        return ApiResponse.ok("Authors fetched", authors);
    }

    @PostMapping("/{id}/discount")
    public ApiResponse<List<BookResponse>> applyDiscount(
            @PathVariable Long id,
            @Valid @RequestBody DiscountRequest request) {
        List<BookResponse> books = bookService.applyAuthorDiscount(id, request.percent()).stream()
                .map(BookResponse::from)
                .toList();

        return ApiResponse.ok("Discount applied", books);
    }
}
```

---

## 5. The Disaster Reproduced: Without vs With `@Transactional`

Let's test author 2 (**Robert C. Martin**) from our seeded database:
- **Clean Code**: Current price = $42.50, Cost price = $15.00
- **Clean Architecture**: Current price = $39.99, Cost price = $22.00

Calculate a **46% discount**:
- Clean Code: `$42.50 * (1 - 0.46) = $22.95`. Since $22.95 is above $15.00, this passes.
- Clean Architecture: `$39.99 * (1 - 0.46) = $21.59`. Since $21.59 is **below** $22.00, this violates the price floor!

### What happens WITHOUT `@Transactional`?
1. The loop updates **Clean Code** to $22.95 and executes `bookRepository.save(book)`. Because each repository call runs in its own auto-commit transaction, **Clean Code is permanently saved in the database**.
2. The loop proceeds to **Clean Architecture**, detects the violation, and throws `ApiException.conflict(...)`.
3. The client receives an error response:
   ```json
   {
     "success": false,
     "message": "A 46% discount would push 'Clean Architecture' below its cost price"
   }
   ```
4. But when inspecting the database (`GET /api/v1/books?authorId=2`), **Clean Code was already modified to $22.95**!
   This leaves the database in a **corrupted, half-applied state**: the user was told the discount failed, yet a book was discounted anyway!

### What happens WITH `@Transactional`?
1. Spring intercepts the call using a dynamic proxy and starts a single database transaction.
2. Changes to Clean Code and Clean Architecture are staged within the active transaction.
3. When `ApiException` escapes the method, Spring's proxy catches the exception and issues an SQL `ROLLBACK`.
4. Neither Clean Code nor Clean Architecture is modified. The database remains completely intact!

---

## 6. How Spring Manages Transactions

Spring manages transactions through **AOP (Aspect-Oriented Programming) Proxies**:

```text
Client / Controller
       │
       ▼
┌──────────────────────────────────────────────┐
│ Spring Proxy (Transaction Interceptor)       │
│                                              │
│ 1. Open Database Transaction                 │
│ 2. Delegate call to actual BookService       │
│    ┌──────────────────────────────────────┐  │
│    │ BookService.applyAuthorDiscount(...) │  │
│    └──────────────────────────────────────┘  │
│ 3. On success: Commit Transaction            │
│ 4. On RuntimeException: Rollback Transaction │
└──────────────────────────────────────────────┘
```

### The Rollback Rules

| Exception Type | Default Spring Behavior | Explanation |
| :--- | :--- | :--- |
| `RuntimeException` (e.g., `ApiException`, `NullPointerException`) | **ROLLBACK** | Unchecked exceptions are treated as unexpected system/domain failures. |
| `Error` (e.g., `OutOfMemoryError`) | **ROLLBACK** | Serious JVM errors trigger a rollback. |
| **Checked Exception** (e.g., `Exception`, `IOException`, `SQLException`) | **COMMIT ANYWAY (!)** | By default, checked exceptions do **not** trigger a rollback! |

> [!WARNING]
> Because checked exceptions commit by default, always either:
> 1. Use unchecked domain exceptions (extending `RuntimeException`, like our `ApiException`), OR
> 2. Explicitly configure rollback: `@Transactional(rollbackFor = Exception.class)`.

---

## 7. Common Pitfall: The Self-Invocation Blind Spot

Because `@Transactional` relies on Spring proxies, calling a `@Transactional` method from another method **inside the same class** bypasses the proxy:

```java
@Service
public class OrderService {

    public void processOrder() {
        // Direct method call: "this.saveOrder()"
        // Bypasses Spring's proxy! @Transactional is IGNORED!
        saveOrder(); 
    }

    @Transactional
    public void saveOrder() {
        // ...
    }
}
```

To ensure `@Transactional` is invoked through the proxy:
- Call `@Transactional` methods from other beans (e.g., Controller -> Service).
- Keep `@Transactional` entry points `public`.

---

## 8. Verifying with curl

Start the application:
```bash
./mvnw spring-boot:run
```

### 1. Test a valid discount (10%)
```bash
curl -X POST http://localhost:8080/api/v1/authors/1/discount \
  -H "Content-Type: application/json" \
  -d '{"percent": 10}'
```
Response (`200 OK`):
```json
{
  "success": true,
  "message": "Discount applied",
  "data": [
    {
      "id": 1,
      "title": "Effective Java",
      "author": { "id": 1, "name": "Joshua Bloch" },
      "genres": ["Java", "Programming"],
      "price": 49.49
    }
  ]
}
```

### 2. Test an invalid discount violating the price floor (46%)
```bash
curl -i -X POST http://localhost:8080/api/v1/authors/2/discount \
  -H "Content-Type: application/json" \
  -d '{"percent": 46}'
```
Response (`409 Conflict`):
```json
{
  "success": false,
  "message": "A 46% discount would push 'Clean Architecture' below its cost price"
}
```

### 3. Verify Atomicity (Nothing Changed)
Verify that Clean Code (id 2) was **not** discounted and still has its original price:
```bash
curl -s http://localhost:8080/api/v1/books/2 | jq .data.price
```
Output:
```text
42.50
```

The database was not left in a partial state. The entire transaction rolled back cleanly!

---

## Summary Table

| Key / Annotation | Why we use it |
| :--- | :--- |
| `@Transactional` | Wraps method execution in a database transaction; commits on return and rolls back on `RuntimeException`. |
| `rollbackFor = Exception.class` | Instructs Spring to roll back even on checked exceptions if needed. |
| `@Transactional(readOnly = true)` | Optimizes queries by preventing Hibernate dirty checking on read operations. |
| `Proxy Pattern` | Spring's mechanism for intercepting method calls to manage transaction lifecycles. |
| `ApiException` | Extends `RuntimeException`, ensuring business rule violations trigger automatic rollbacks. |

Next: [**Tutorial 13 — Testing**](tutorial-13%20%28Testing%29.md)

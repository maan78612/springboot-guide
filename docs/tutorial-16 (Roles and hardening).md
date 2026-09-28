# Tutorial 16 — Roles and hardening

Apply authorization, ownership checks, and hardened security defaults.

Files for this stage:

- Updated: `src/main/java/com/example/bookshop/model/Book.java`
- Updated: `src/main/java/com/example/bookshop/service/BookService.java`
- Updated: `src/main/java/com/example/bookshop/controller/BookController.java`
- Updated: `src/main/java/com/example/bookshop/config/SecurityConfig.java`
- Updated: `src/main/java/com/example/bookshop/exception/GlobalExceptionHandler.java`

---

## 1. Protect by role

```java
/** POST /api/v1/books/{id}/restore — restore a deleted book (admin only). */
/*
 * | Key                       | Why we use it                              |
 * |---------------------------|--------------------------------------------|
 * | @PreAuthorize             | Checks access before invoking the method   |
 * | hasRole('ADMIN')          | Requires the caller to have the admin role |
 */
@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/{id}/restore")
public ApiResponse<BookResponse> restoreBook(@PathVariable Long id) {
    // ...
}
```

This enforces admin-only access on sensitive endpoints.

## 2. Check ownership in the service

```java
/*
 * | Key                     | Why we use it                                  |
 * |-------------------------|------------------------------------------------|
 * | book.getOwner() == null | Seeded house-stock books are admin-only        |
 * | owner id comparison     | Allows the seller to change only their own book|
 * | ApiException.forbidden  | Returns HTTP 403 when ownership fails          |
 */
if (book.getOwner() == null || !book.getOwner().getId().equals(actor.getId())) {
    throw ApiException.forbidden("You can only modify books you created");
}
```

The controller passes identity; the service decides whether the user may modify the row.

## 3. Add rate limiting and CORS

The security config adds auth-rate limiting, CORS rules, and secure headers for browser clients.

Next: [**Tutorial 17 — API documentation**](tutorial-17%20%28API%20documentation%29.md)

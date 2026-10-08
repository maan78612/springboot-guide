# Tutorial 16 — Roles and hardening

Apply authorization, ownership checks, and hardened security defaults. Learn how to restrict endpoints by role with `@PreAuthorize`, implement fine-grained resource ownership in the service layer, configure CORS, and rate-limit sensitive authentication routes.

Files for this stage:

- Updated: `src/main/java/com/example/bookshop/model/Book.java` (add `owner` relationship)
- Updated: `src/main/java/com/example/bookshop/service/BookService.java` (ownership validation)
- Updated: `src/main/java/com/example/bookshop/controller/BookController.java` (`@AuthenticationPrincipal`, `@PreAuthorize`)
- Updated: `src/main/java/com/example/bookshop/config/SecurityConfig.java` (`@EnableMethodSecurity`, CORS, rate limiting)
- Updated: `src/main/java/com/example/bookshop/exception/GlobalExceptionHandler.java` (handle `AccessDeniedException`)

---

## 1. Authorization: Two Layers of Protection

```text
┌─────────────────────┬────────────────────────────────────────────────────────┐
│ Mechanism           │ Scope & Question                                       │
├─────────────────────┼────────────────────────────────────────────────────────┤
│ Role Check          │ "May only ADMINs access this endpoint?" (URL / method) │
│ Ownership Check     │ "May this seller edit this specific book?" (Row-level) │
└─────────────────────┴────────────────────────────────────────────────────────┘
```

The bookshop acts as a marketplace:
- Standard users (`USER`) are sellers who can create books and modify **only the books they listed**.
- Pre-seeded books (`owner = null`) represent house inventory that only administrators (`ADMIN`) may modify.
- Administrators can manage, delete, or restore any book.

---

## 2. Protect Endpoints by Role with `@PreAuthorize`

Enable method security on `SecurityConfig.java` with `@EnableMethodSecurity`:

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    // ...
}
```

Now annotate administrative controller endpoints with `@PreAuthorize`:

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
    Book restored = bookService.restoreBook(id);
    return ApiResponse.ok("Book restored", BookResponse.from(restored));
}
```

This enforces admin-only access on sensitive endpoints.

> [!IMPORTANT]
> **Catching Method-Security Denials**:
> When a user fails a `@PreAuthorize` rule, Spring throws an `AccessDeniedException` inside the controller call. Make sure `GlobalExceptionHandler` explicitly catches `AccessDeniedException` to return an HTTP `403 Forbidden` response in the standard envelope:
> ```java
> @ExceptionHandler(AccessDeniedException.class)
> public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
>     return ResponseEntity.status(HttpStatus.FORBIDDEN)
>             .body(ErrorResponse.of("You do not have permission to perform this action"));
> }
> ```

---

## 3. Check Row-Level Ownership in the Service Layer

Controllers extract the caller's identity from the JWT and pass it to the service method as an explicit parameter. The service evaluates whether the actor owns the row:

In `src/main/java/com/example/bookshop/model/Book.java`:
```java
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private UserAccount owner;
```

In `src/main/java/com/example/bookshop/service/BookService.java`:
```java
/*
 * | Key                     | Why we use it                                  |
 * |-------------------------|------------------------------------------------|
 * | book.getOwner() == null | Seeded house-stock books are admin-only        |
 * | owner id comparison     | Allows the seller to change only their own book|
 * | ApiException.forbidden  | Returns HTTP 403 when ownership fails          |
 */
private void assertCanModify(Book book, UserAccount actor) {
    if (actor.getRole() == Role.ADMIN) {
        return; // Admins may modify any book
    }

    if (book.getOwner() == null || !book.getOwner().getId().equals(actor.getId())) {
        throw ApiException.forbidden("You can only modify books you created");
    }
}
```

The controller passes identity; the service decides whether the user may modify the row.

### Key Architectural Decision
Identity travels as an explicit parameter (`String actorEmail`), rather than being extracted from a static security context (`SecurityContextHolder`) inside the service. This keeps `BookService` completely testable with plain JUnit 5 and Mockito.

---

## 4. Add Rate Limiting and CORS

The security config adds auth-rate limiting, CORS rules, and secure headers for browser clients.

### 1. Cross-Origin Resource Sharing (CORS)
Browsers block web applications on `http://localhost:3000` from reading API responses from `http://localhost:8080` unless the server explicitly grants access. Configure allowed origins in `SecurityConfig`:

```java
@Bean
CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of("http://localhost:3000"));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

### 2. Rate Limiting Sensitive Endpoints
Authentication endpoints (`/api/v1/auth/**`) are frequent targets for brute-force credential stuffing. We introduce a rate-limiting filter (e.g. limiting clients to 10 requests per minute per IP address):
- Exceeding the threshold immediately returns `HTTP 429 Too Many Requests`:
  ```json
  {
    "success": false,
    "message": "Too many attempts. Try again in a minute."
  }
  ```

---

## 5. Standard Security Headers

Spring Security automatically applies defensive HTTP response headers:

| Header | Value | Purpose |
| :--- | :--- | :--- |
| `X-Content-Type-Options` | `nosniff` | Prevents MIME-type sniffing attacks. |
| `Cache-Control` | `no-cache, no-store, max-age=0` | Prevents caching of authenticated user data. |
| `X-Frame-Options` | `SAMEORIGIN` / `DENY` | Protects against Clickjacking attacks inside iframes. |
| `X-XSS-Protection` | `0` | Disables legacy, buggy browser XSS filters in favor of CSP. |

---

## Summary Table

| Annotation / Concept | Scope | Why we use it |
| :--- | :--- | :--- |
| `@PreAuthorize("hasRole('ADMIN')")` | Controller method | Guards entire endpoints based on caller role. |
| `AccessDeniedException` | MVC Exception Handler | Converts Spring Security method denials to HTTP 403 JSON envelope. |
| `assertCanModify(book, actor)` | Domain Service | Enforces row-level record ownership rules. |
| `CorsConfigurationSource` | HTTP Filter | Permits approved browser origins while rejecting malicious domains. |
| `HTTP 429 Too Many Requests` | Rate Limiter | Protects login and registration routes against brute-force attacks. |

Next: [**Tutorial 17 — API documentation**](tutorial-17%20%28API%20documentation%29.md)

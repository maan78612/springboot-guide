# Tutorial 16 — Roles and hardening

user/admin roles, ownership checks, CORS, rate limiting, and
security headers.

Files for this stage:
- `model/Book.java` (+ owner), `service/BookService.java`
  (ownership rules, identity as a parameter)
- `controller/BookController.java`, `AuthorController.java`
  (@AuthenticationPrincipal, @PreAuthorize)
- `config/SecurityConfig.java` (@EnableMethodSecurity, CORS bean,
  rate-limit filter), `config/RateLimitFilter.java` (new)
- `exception/GlobalExceptionHandler.java` (+ AccessDeniedException)
- unit tests for the ownership rules

---

## 1. Authorization: two layers of "no"

Tutorial 15 answered WHO you are. Now: what may you do?

```
+---------------------+--------------------------------------------+
| Role check          | "only ADMINs may call this endpoint"       |
| Ownership check     | "only the seller who LISTED this book      |
|                     |  (or an admin) may change it"              |
+---------------------+--------------------------------------------+
```

The bookshop is now a small marketplace: registered users are
sellers, a book remembers its `owner`, and the seeded books
(`owner = null`) are house stock only admins may touch — the same
model as products in the node-mongo-helper reference.

## 2. Role checks: URL rules + method rules

Two mechanisms, used together:

**URL level** (SecurityConfig): `.requestMatchers(GET,
"/api/v1/books/deleted").hasRole("ADMIN")` — the filter chain
rejects before any code runs.

**Method level**: `@EnableMethodSecurity` once, then:

```java
@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/{id}/restore")
public ApiResponse<BookResponse> restoreBook(...) { ... }
```

The admin-only surface: `GET /books/deleted`, `POST /{id}/restore`,
`POST /authors/{id}/discount`. Where does `hasRole('ADMIN')` come
from? Login puts `"role": "ADMIN"` in the token; the converter from
tutorial 15 turns it into a `ROLE_ADMIN` authority; `hasRole`
matches it.

**A real bug this stage found:** a seller calling the discount
endpoint got **500**, not 403. URL-rule denials happen in the FILTER
(handled by ApiAuthErrorHandler), but `@PreAuthorize` denials are
thrown INSIDE the controller call as `AccessDeniedException` — which
fell through to the catch-all "bug" handler. The fix is an explicit
handler in the advice, above the catch-all. Verified after:

```
POST /authors/2/discount (seller token)
{"success":false,"message":"You do not have permission to perform this action"} | 403
```

## 3. Ownership: identity is a parameter

Controllers hand the service WHO is acting; the service decides:

```java
// controller
bookService.deleteBook(id, jwt.getSubject());

// service
private void assertCanModify(Book book, UserAccount actor) {
    if (actor.getRole() == Role.ADMIN) return;
    if (book.getOwner() == null || !book.getOwner().getId().equals(actor.getId()))
        throw ApiException.forbidden("You can only modify books you created");
}
```

Design choices worth copying:

- **Identity travels as an explicit method parameter**, not fished
  from a hidden static context deep in the service. That is why the
  ownership rules have plain-Mockito unit tests (two new ones — a
  non-owner is refused, an admin is not).
- **Role is read from the DATABASE row, not the token claim.** A
  demoted admin loses power on their next request — not whenever
  their old token happens to expire.

Verified end to end:

```
seller creates book            -> 201 (id 6, owner = seller)
seller updates OWN book        -> 200
seller updates SEEDED book 1   -> 403 "You can only modify books you created"
admin  updates seeded book 1   -> 200
seller GET /books/deleted      -> 403   |   admin -> 200
```

## 4. Rate limiting the auth endpoints

Login is the endpoint attackers hammer. `RateLimitFilter` — ~30
lines, no dependencies — allows N auth requests per IP per minute
(configured: `bookshop.security.auth-rate-limit-per-minute=10`),
then answers 429. Verified by hammering login:

```
401 401 401 401 401 401 401 429 429 429 429 429
{"success":false,"message":"Too many attempts. Try again in a minute."}
```

(It tripped after 7 here because earlier logins in the same minute
had already spent budget — per-IP windows do that.)

Two honest limitations, in the file header too: counters are
per-process (multi-instance setups need Redis or a gateway), and
behind a proxy you must read `X-Forwarded-For`, not
`getRemoteAddr()`. Same caveats as the in-memory default of
express-rate-limit in the Node repo.

One wiring subtlety that produced this stage's second bug: the
filter is deliberately NOT `@Component`. As a scanned bean it broke
the `@WebMvcTest` slice (the slice instantiates filters but not
`@ConfigurationProperties`) — real error:
`Parameter 0 of constructor in ...RateLimitFilter required a bean of
type 'BookshopProperties' that could not be found`. And a `Filter`
bean also gets registered globally by Boot, running twice.
SecurityConfig now constructs it and adds it to the chain itself.

## 5. CORS

> **CORS** (Cross-Origin Resource Sharing): browsers refuse to let
> a page from origin A read responses from origin B unless B's
> headers say A is welcome.

It protects browser users — curl and servers ignore it entirely; it
is NOT authentication. Config allows origins from
`bookshop.security.cors-allowed-origins` (dev default
`http://localhost:3000`, where a frontend dev server would live).
Verified both directions:

```
OPTIONS with Origin: http://localhost:3000
  -> 200, Access-Control-Allow-Origin: http://localhost:3000

OPTIONS with Origin: https://evil.example.com
  -> 403, no allow headers (the browser blocks the page's request)
```

## 6. Security headers

Spring Security adds protective headers by default — verified on a
plain GET:

```
X-Content-Type-Options: nosniff     don't guess content types
Cache-Control: no-cache, no-store   auth'd responses aren't cached
X-Frame-Options: SAMEORIGIN         no strangers framing our pages
X-XSS-Protection: 0                 legacy filter off (modern advice)
```

We changed exactly one (SAMEORIGIN instead of DENY, for the dev H2
console). HSTS — forcing HTTPS — activates when requests actually
arrive over HTTPS, which is tutorial 19 territory (behind a proxy).

## 7. The common mistakes

1. **Expecting one error path for all denials.** URL rules fail in
   the filter; method rules fail in the controller layer. Ours now
   both answer a 403 envelope — but only because BOTH handlers
   exist. (The 500 we shipped for an hour proves it.)
2. **Trusting the token's role claim forever.** Read the role from
   the database per request; tokens outlive demotions.
3. **Checking ownership in the controller.** It belongs in the
   service with the other business rules — controllers only carry
   identity in.
4. **Registering a security filter as @Component** — double
   registration and broken test slices, as found above.

## 8. Recap

- Roles gate endpoints (URL rule + @PreAuthorize, defense in
  depth); ownership gates rows (service rule, unit-tested).
- Identity flows controller → service as a parameter; role truth
  lives in the database.
- Auth endpoints are rate limited per IP; 429 in the envelope.
- CORS whitelists browser origins from config; headers come secure
  by default.

Next: **Tutorial 17 — API documentation**: OpenAPI, Swagger UI, and
importing the spec into Postman.

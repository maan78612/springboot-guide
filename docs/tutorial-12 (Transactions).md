# Tutorial 12 — Transactions

@Transactional, when a rollback actually happens (verified four
ways), and the lazy-loading trap.

Files for this stage:
- `service/BookService.java` (+ applyAuthorDiscount)
- `controller/AuthorController.java` (+ POST /{id}/discount)
- `dto/DiscountRequest.java` (new)

---

## 1. Why: some work is all-or-nothing

> A **transaction** is a group of database operations that succeed
> together or leave no trace at all — never half.

The bank-transfer example is classic (debit A, credit B — imagine
only the debit happening). Ours is bulk pricing: *discount every
book by one author*, with the rule that no price may drop below that
book's cost. If book 2 breaks the rule after book 1 was already
updated, book 1's update must be undone.

New endpoint to carry the lesson:

```
POST /api/v1/authors/{id}/discount     {"percent": 46}
```

## 2. The disaster, reproduced (no transaction)

`applyAuthorDiscount` first shipped WITHOUT `@Transactional`. Author
2 has Clean Code (42.50, cost 15.00) and Clean Architecture (39.99,
cost 22.00). A 46% discount prices them at 22.95 (fine) and 21.59
(below 22.00 — violation):

```
POST /api/v1/authors/2/discount {"percent":46}
{"success":false,"message":"A 46% discount would push 'Clean
 Architecture' below its cost price"} | HTTP 409

GET /api/v1/books?authorId=2
[('Clean Code', '22.95'), ('Clean Architecture', '39.99')]
```

Read that carefully: the client was told the discount FAILED, and
Clean Code is discounted anyway. Without a transaction, each
`save()` ran in its own tiny transaction and committed immediately;
the exception only stopped the loop. This is the worst kind of bug:
an error message AND corrupted data.

## 3. The fix: one annotation

```java
@Transactional
public List<Book> applyAuthorDiscount(Long authorId, int percent) { ... }
```

`@Transactional` makes Spring wrap the METHOD in one transaction:
begin before the first line, commit after the last, roll back if a
`RuntimeException` escapes. Same request, verified again:

```
409 (same message)
GET /api/v1/books?authorId=2
[('Clean Code', '42.5'), ('Clean Architecture', '39.99')]     <- untouched
```

Same code inside, opposite outcome. How it works matters for the
trap section below: Spring injects a PROXY — a wrapper object —
instead of your service. The proxy opens the transaction, calls your
method, then commits/rolls back. (This is also why our
`ApiException` being a RuntimeException was the right call in
tutorial 09: business rule failures roll back automatically.)

Where `@Transactional` belongs: on SERVICE methods — the service
defines the unit of work. Repositories are too small (one query),
controllers too big (HTTP does not belong in a transaction). Reads
can use `@Transactional(readOnly = true)` for consistency + a small
performance hint; single-query methods work fine without any
annotation because Spring Data repositories are transactional per
call — which is exactly why the un-annotated version half-committed.

## 4. The rollback rules — including the nasty one

```
+--------------------------------------+------------------------------+
| What escapes the method              | Default behavior             |
+--------------------------------------+------------------------------+
| RuntimeException (ApiException, NPE) | ROLLBACK                     |
| Error (OutOfMemoryError...)          | ROLLBACK                     |
| CHECKED exception (Exception, IO...) | COMMIT ANYWAY (!)            |
+--------------------------------------+------------------------------+
```

That third row sounds unbelievable, so I verified it: same method,
`@Transactional` PRESENT, but the rule throwing
`throw new Exception("CHECKED...")` instead of ApiException:

```
POST .../discount {"percent":46}   -> HTTP 500
GET  /api/v1/books?authorId=2
[('Clean Code', '22.95'), ('Clean Architecture', '39.99')]    <- COMMITTED
```

The transaction machinery saw a checked exception and committed
everything done so far. (Historical Java-EE reasoning: checked
exceptions were assumed to be "business outcomes you handle".) The
fixes: throw runtime exceptions for failures — our `ApiException`
approach — or, if a checked exception must cross a transactional
method, declare `@Transactional(rollbackFor = Exception.class)`.

## 5. The lazy-loading trap

Tutorial 10 made relationships LAZY: touching `book.getGenres()`
runs a query *if a database session is still open*. Who keeps it
open? Every startup has been logging this warning:

```
WARN JpaBaseConfiguration$JpaWebConfiguration : spring.jpa.open-in-view
is enabled by default ...
```

**Open Session In View (OSIV)**: Spring Boot keeps the JPA session
open for the WHOLE web request, so lazy loading works even in the
controller (our `BookResponse.from` touches genres there). Verified
by turning it off — `spring.jpa.open-in-view=false`:

```
GET /api/v1/books   -> HTTP 500
log: LazyInitializationException: Cannot lazily initialize collection
     of role 'com.example.bookshop.model.Book.genres'...

GET /api/v1/authors -> HTTP 200   (unaffected - its query FETCH JOINS books)
```

The books endpoint died because the session closed when the service
returned, and the controller then touched an unfetched lazy
collection. The authors endpoint survived because tutorial 10's
fetch join loaded everything up front — the discipline OSIV-off
demands: **each query fetches everything its use case will touch.**

Our decision: keep OSIV on (the Boot default) for this project —
right for a small app and while learning. Know the trade-off: it
holds a DB connection for the whole request and hides fetching
mistakes; large-scale teams often disable it and pay with stricter
fetch discipline. When you see `LazyInitializationException` in the
wild: something touched a lazy relation after its session closed —
fetch it in the query (join fetch / @EntityGraph), or map to DTOs
inside the transactional service.

## 6. The common mistake — self-invocation (the proxy blind spot)

Because the transaction lives in the PROXY around your bean, a
method calling ANOTHER method of the same class bypasses the proxy —
`this.helper()` is a plain Java call, and `@Transactional` on
`helper()` does nothing in that call. Symptoms: "my annotation is
ignored". Same blind spot applies inside: only calls that arrive
FROM OUTSIDE the bean pass through the proxy. Keep transactional
entry points public, on the service, called from other beans — as
all of ours are.

## 7. Recap

- A transaction = all-or-nothing. `@Transactional` on the service
  method that IS the unit of work.
- Without it, each repository call commits alone — error message
  plus half-saved data, as reproduced.
- Rollback on runtime exceptions only; checked exceptions COMMIT
  (verified!) unless `rollbackFor` says otherwise.
- OSIV (on by default) lets lazy loading reach the controller;
  without it you must fetch per-query. LazyInitializationException =
  lazy touch after session close.
- Annotations live on proxies: self-calls bypass them.

Next: [**Tutorial 13 — Testing**](tutorial-13%20%28Testing%29.md): Mockito unit tests for the rules we
just wrote, @WebMvcTest for the web layer, @SpringBootTest for the
whole thing.

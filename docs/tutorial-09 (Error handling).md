# Tutorial 09 — Error handling

Custom exceptions, @RestControllerAdvice, and one consistent error
shape for the whole API.

Files for this stage:
- `exception/ApiException.java` (new)
- `exception/GlobalExceptionHandler.java` (new)
- `dto/ErrorResponse.java` (new)
- `service/BookService.java` (throws instead of Optional)
- `controller/BookController.java` (error plumbing removed)

---

## 1. Why: errors are part of the API contract

So far our failures were a mess: 404s with empty bodies, validation
errors buried in a stack trace, and the default error JSON that
changes shape depending on what broke. A client cannot program
against that. The goal:

```json
{ "success": false, "message": "human-readable reason",
  "errors": [ {"field": "...", "message": "..."} ] }        // only when field-level
```

— for EVERY failure, no matter where it happened. (`ErrorResponse`
is the failure twin of tutorial 07's `ApiResponse`; same contract as
the Node reference.)

One distinction drives the whole design:

```
+---------------------------+-----------------------------------------+
| Operational errors        | Bugs                                    |
+---------------------------+-----------------------------------------+
| expected in normal use:   | never expected: NPE, broken SQL,        |
| unknown id, bad input,    | connection leak                         |
| no permission             |                                         |
| tell the client exactly   | tell the client NOTHING specific        |
| what and why              | (500 + generic text), tell the LOG      |
|                           | everything                              |
+---------------------------+-----------------------------------------+
```

## 2. ApiException — the error our code throws on purpose

```java
throw ApiException.notFound("Book with id " + id + " not found");
```

One small class: a `RuntimeException` carrying an `HttpStatus`, with
a static factory per status we use (`badRequest`, `unauthorized`,
`forbidden`, `notFound`, `conflict`) — a direct port of `ApiError`
from the Node reference. Unchecked on purpose: services throw it
anywhere, no `throws` clauses, and it flies up on its own.

The service got BETTER by using it — compare `getBookById`:

```java
// before                                   // after
public Optional<Book> getBookById(Long id)  public Book getBookById(Long id) {
                                                return bookRepository.findById(id)
                                                    .orElseThrow(() -> ApiException
                                                        .notFound("Book with id " + id + " not found"));
                                            }
```

Callers get a real `Book`, never a maybe. `updateBook` and
`deleteBook` now just call it and cannot forget the missing-id case.
And the controller shed all its `.map(...).orElseGet(...)` plumbing —
every method is one sentence again. (`Optional` remains the right
type at the repository edge; the service is where "maybe" becomes
either a value or a thrown error.)

## 3. @RestControllerAdvice — the single translation point

> **@RestControllerAdvice** marks a bean whose `@ExceptionHandler`
> methods apply to ALL controllers. Any exception escaping a
> controller lands there, and Spring picks the most specific
> matching handler.

This is `error.middleware.js` in Spring clothes. Ours handles eight
cases; each one is a few lines of "status + ErrorResponse":

```
+------------------------------------+------+--------------------------------+
| Exception                          | Code | Meaning                        |
+------------------------------------+------+--------------------------------+
| ApiException                       | its  | our own thrown errors          |
|                                    | own  |                                |
| MethodArgumentNotValidException    | 400  | @Valid failed -> field list    |
| HttpMessageNotReadableException    | 400  | body is not valid JSON         |
| MethodArgumentTypeMismatchException| 400  | /books/abc, id must be a number|
| NoResourceFoundException           | 404  | URL matches no endpoint        |
| HttpRequestMethodNotSupportedExc.  | 405  | right URL, wrong verb          |
| HttpMediaTypeNotSupportedException | 415  | missing/wrong Content-Type     |
| Exception (the catch-all, LAST)    | 500  | a bug: hide details, log stack |
+------------------------------------+------+--------------------------------+
```

The validation handler is where tutorial 08's messages finally reach
the client — it copies each field error into `errors[]`.

## 4. Verified — every path, one shape

All real output:

```
GET  /api/v1/books/999
{"success":false,"message":"Book with id 999 not found"} | 404

POST /api/v1/books  {"title":"","price":-5}
{"success":false,"message":"Validation failed","errors":[
  {"field":"title","message":"title is required"},
  {"field":"price","message":"price must be greater than 0"},
  {"field":"author","message":"author is required"}]} | 400

POST broken JSON        -> {"success":false,"message":"Malformed JSON in request body"} | 400
GET  /api/v1/books/abc  -> {"success":false,"message":"Parameter 'id' has an invalid value: 'abc'"} | 400
GET  /api/v1/nothing    -> {"success":false,"message":"No endpoint GET /api/v1/nothing"} | 404
DELETE /api/v1/books    -> {"success":false,"message":"DELETE is not allowed here"} | 405
POST no Content-Type    -> {"success":false,"message":"Content-Type must be application/json"} | 415
```

And the bug case — I planted `throw new IllegalStateException(
"pretend NPE: connection pool leaked")` inside the service:

```
what the CLIENT saw:
{"success":false,"message":"Something went wrong. Please try again later."} | 500

what OUR LOG got:
ERROR ... GlobalExceptionHandler : Unhandled exception
java.lang.IllegalStateException: pretend NPE: connection pool leaked
    at ... (full stack trace)
```

Internal details (class names, table names, file paths) are useful
to attackers and useless to users — the 500 handler is deliberately
vague outward and deliberately loud inward. If you forget the
`log.error` line you will debug production blind; it is the most
important line in the file.

## 5. The common mistakes

1. **`@ExceptionHandler` methods inside a controller class.** Works —
   but only for that one controller. Six controllers later, six
   half-copies of error handling disagree. Global shapes belong in
   the advice class.
2. **Assuming the advice catches everything.** It only sees
   exceptions from the controller layer downwards. Exceptions thrown
   in security FILTERS (tutorial 16) happen before any controller
   exists and never reach it — they need separate handling. Noted
   now so it does not surprise us then.
3. **Catch-all that answers with `ex.getMessage()`.** That ships raw
   internals ("could not open JDBC connection to db:5432") to
   strangers. The catch-all's message is a constant string; the
   details go to the log.

## 6. Recap

- Two error families: operational (explain honestly, right status)
  vs bugs (generic 500 outward, full stack in the log).
- Services throw `ApiException.notFound(...)`; nobody maps Optionals
  to 404 by hand anymore.
- One `@RestControllerAdvice` owns every error shape; eight handlers
  cover our real cases; catch-all goes last.
- Every failure now answers `{"success":false, "message", errors?}` —
  same envelope family as every success.

Next: **Tutorial 10 — Relationships and queries**: Author and Genre
entities, OneToMany / ManyToMany, the N+1 problem caught in the SQL
log, derived queries, @Query, and pagination + whitelisted
search/filter/sort with `meta`.

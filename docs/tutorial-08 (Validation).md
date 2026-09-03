# Tutorial 08 — Validation

@Valid, the constraint annotations, and stopping bad data at the
door.

Files for this stage:
- `dto/BookRequest.java` (constraints added)
- `controller/BookController.java` (@Valid on POST and PUT bodies)

The dependency (`spring-boot-starter-validation`) has been in the pom
since tutorial 04, where it validated configuration. Today it
validates requests — same annotations, different door.

---

## 1. Why: the API currently believes anything

Verified before any changes:

```
POST /api/v1/books   {"title":"","price":-5}

HTTP 201
{"success":true,"message":"Book created",
 "data":{"id":5,"title":"","author":null,"price":-5}}
```

An empty title, no author, a negative price — created, stored,
"success". Every consumer downstream now has to cope with that row
forever. The fix is to reject bad input at the boundary, before it
touches the service, in one declarative place.

Rule of thumb: **validate where data ENTERS** (the request DTO), not
deep inside, and not in six different if-chains per controller.

## 2. Constraints go on the request DTO

`BookRequest` now declares its door policy:

```java
public record BookRequest(

        @NotBlank(message = "title is required")
        @Size(max = 200, message = "title must be at most 200 characters")
        String title,

        @NotBlank(message = "author is required")
        String author,

        @NotNull(message = "price is required")
        @Positive(message = "price must be greater than 0")
        @Digits(integer = 8, fraction = 2,
                message = "price must have at most 2 decimal places")
        BigDecimal price) {
}
```

Write every `message` for a human — tutorial 09 will put these exact
strings into the error response the client reads.

The constraints you will actually use:

```
+---------------------+---------------------------------------------+
| @NotNull            | must not be null ("" passes!)               |
| @NotEmpty           | not null AND length > 0 ("   " passes!)     |
| @NotBlank           | not null AND has visible characters         |
| @Size(min=, max=)   | string/collection length range              |
| @Positive           | number > 0   (@PositiveOrZero for >= 0)     |
| @Min(n) / @Max(n)   | number range                                |
| @DecimalMin("0.0")  | like @Min but for BigDecimal precision      |
| @Digits(integer=8,  | max digits before / after the decimal       |
|         fraction=2) | point - the money-shape check               |
| @Email              | roughly-valid email address                 |
| @Pattern(regexp=)   | custom regex, the escape hatch              |
+---------------------+---------------------------------------------+
```

Choosing between the three "not empty" ones: strings almost always
want `@NotBlank`. Numbers and objects take `@NotNull` plus range
rules (`@NotBlank` means nothing for a `BigDecimal`).

## 3. @Valid arms the constraints

Annotations on the DTO are only half the mechanism. The controller
parameter must ask for validation:

```java
public ResponseEntity<ApiResponse<BookResponse>> createBook(
        @Valid @RequestBody BookRequest request) { ... }
```

Now Spring runs all constraints after parsing the JSON and BEFORE
your method body. Any violation → the method never runs, the client
gets a 400.

Verified, same garbage as before:

```
POST /api/v1/books   {"title":"","price":-5}

HTTP 400
{"timestamp":"...","status":400,"error":"Bad Request",
 "trace":"...MethodArgumentNotValidException: ... with 3 errors:
  [Field error ... on field 'author': ... default message [author is required]]
  [Field error ... on field 'title':  ... (title is required)] ..."}
```

All three violations were caught in one pass (validation reports
everything wrong, not just the first hit). But look at the response
shape: the useful field errors are buried inside a Java stack trace
in `trace`, and the body is not our envelope. A frontend cannot work
with this. Extracting those field errors into
`{"success":false, "errors": [...]}` is exactly tutorial 09.

And a valid request still flows:

```
POST {"title":"Test-Driven Development","author":"Kent Beck","price":39.99}
HTTP 201
```

## 4. The common mistake — constraints without @Valid

I removed just `@Valid` from the POST parameter, kept every
annotation on `BookRequest`, and resent the garbage:

```
HTTP 201
{"success":true,"message":"Book created",
 "data":{"id":4,"title":"","author":null,"price":-5}}
```

**Everything annotated, nothing checked, no warning anywhere.** The
constraints are inert metadata until a `@Valid` on the parameter
arms them. When "validation doesn't work", check for the missing
`@Valid` before you doubt the annotations — it is almost always
this.

(Related trap for later: `@Valid` on a nested object inside a DTO is
also needed to validate its insides — we already do this in
`BookshopProperties` with `@Valid private final Catalog catalog`.)

## 5. Where business rules do NOT go

`@NotBlank` answers "is this input well-formed?". It cannot answer
"do we already stock this ISBN?" or "may THIS user delete THAT
book?" — those need the database or the logged-in user, and they
live in the SERVICE layer (tutorials 10 and 16). Boundary checks
shape, service checks meaning.

## 6. Recap

- Constraints live on the request DTO, with human-readable messages.
- `@Valid` on the controller parameter arms them; forgetting it is
  silent and common.
- Violations → 400 before your code runs; all errors reported at
  once.
- The default 400 body is unusable for clients — fixing that shape
  is next.

Next: **Tutorial 09 — Error handling**: custom exceptions,
@RestControllerAdvice, and one `{"success":false}` error shape for
the whole API.

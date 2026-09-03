# Tutorial 06 — A full CRUD API

GET / POST / PUT / DELETE, @PathVariable, @RequestBody, and the right
HTTP status code for each case.

Files for this stage:
- `service/BookService.java` (CRUD methods)
- `controller/BookController.java` (all five endpoints)

---

## 1. Why: CRUD and HTTP were made for each other

CRUD = Create, Read, Update, Delete — the four things you can do to
stored data. HTTP has a verb for each, and a well-behaved API uses
them instead of inventing URLs like `/getBooks` or `/deleteBook?id=4`:

```
+----------+--------------------------+---------+---------------------+
| Verb     | URL                      | Meaning | Success code        |
+----------+--------------------------+---------+---------------------+
| GET      | /api/v1/books            | list    | 200 OK              |
| GET      | /api/v1/books/{id}       | read    | 200 OK              |
| POST     | /api/v1/books            | create  | 201 Created         |
| PUT      | /api/v1/books/{id}       | replace | 200 OK              |
| DELETE   | /api/v1/books/{id}       | remove  | 204 No Content      |
+----------+--------------------------+---------+---------------------+
```

The URL names the THING (books), the verb says what to DO to it.

Status code rules of thumb, worth memorizing once:

```
+-------+---------------------------------------------------------+
| 200   | done, here is the result                                |
| 201   | created; a Location header says where it now lives      |
| 204   | done, and there is deliberately nothing to say (DELETE) |
| 400   | your request is broken (bad JSON, bad types)            |
| 404   | that id does not exist                                  |
| 415   | you sent a body type I don't accept (Content-Type)      |
| 500   | not your fault - the server has a bug                   |
+-------+---------------------------------------------------------+
```

## 2. The two new annotations

**@PathVariable** — pulls a piece out of the URL:

```java
@GetMapping("/{id}")
public ResponseEntity<Book> getBookById(@PathVariable Long id) { ... }
```

`{id}` in the mapping is a placeholder; `@PathVariable Long id`
receives it, already converted to `Long`. (Send `/books/abc` and
conversion fails → 400, for free.)

**@RequestBody** — parses the JSON request body into an object:

```java
@PostMapping
public ResponseEntity<Book> createBook(@RequestBody Book book) { ... }
```

This is Jackson from tutorial 02 running in reverse: JSON text →
no-arg constructor → setters. Note `id` stays null on a POSTed book —
Book has no id setter, so a client cannot dictate ids; the database
assigns them.

**ResponseEntity<T>** — until now methods returned the body and the
status was always 200. `ResponseEntity` is body + status + headers:

```java
ResponseEntity.of(optional)      // 200 with value, or 404 empty
ResponseEntity.created(uri)      // 201 + Location header
        .body(saved)
ResponseEntity.noContent().build()   // 204
ResponseEntity.notFound().build()    // 404
```

The service returns `Optional<Book>` ("maybe there is no book 42") —
business fact. The controller translates that fact into HTTP (200 vs
404). Each layer speaks its own language, as promised in tutorial 03.

One JPA fact used by update: `save()` does INSERT when the entity
has no id, UPDATE when it has one. Our update loads the entity,
mutates it with setters, saves the same object — the id stays.

## 3. Verified: every endpoint, every code

App running with `SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run`.
Every command below was actually run, output shown as received:

```bash
curl http://localhost:8080/api/v1/books/1
# {"title":"Effective Java","author":"Joshua Bloch","price":54.99,"id":1} | HTTP 200

curl http://localhost:8080/api/v1/books/999
# HTTP 404

curl -i -X POST http://localhost:8080/api/v1/books \
  -H "Content-Type: application/json" \
  -d '{"title":"Refactoring","author":"Martin Fowler","price":47.00}'
# HTTP/1.1 201
# Location: /api/v1/books/4
# {"title":"Refactoring","author":"Martin Fowler","price":47.00,"id":4}

curl -X PUT http://localhost:8080/api/v1/books/4 \
  -H "Content-Type: application/json" \
  -d '{"title":"Refactoring (2nd ed)","author":"Martin Fowler","price":52.00}'
# {"title":"Refactoring (2nd ed)",...,"id":4} | HTTP 200

curl -X PUT http://localhost:8080/api/v1/books/999 -H "Content-Type: application/json" -d '...'
# HTTP 404

curl -X DELETE http://localhost:8080/api/v1/books/4
# HTTP 204

curl -X DELETE http://localhost:8080/api/v1/books/4     # again
# HTTP 404
```

Notice id 4: the seeds took 1–3 without touching the identity
counter, so the first created book slots in cleanly at 4 — the
data.sql design from tutorial 05 doing its job.

Also for free, no code written:

```bash
# broken JSON body
curl -X POST ... -d '{"title": "broken'          # HTTP 400
# body sent without Content-Type: application/json
curl -X POST ... -d '{...}'                      # HTTP 415
```

! Aside, learned while testing: every devtools restart WIPES the
in-memory H2 database and re-runs data.sql. If your data keeps
"disappearing" during development, that is why — it is a feature of
create-drop + in-memory, not a bug.

## 4. The common mistake — forgetting @RequestBody

I removed `@RequestBody` from the POST parameter and sent the same
valid create request. Real result:

```
{"title":null,"author":null,"price":null,"id":4}
HTTP 201
```

**A book of nulls, saved, and reported as a success.** Without the
annotation Spring does not read the body at all — it just constructs
an empty `Book` (that protected no-arg constructor) and binds nothing
into it. No error anywhere. When a POSTed object arrives all-null:
check `@RequestBody` first, before you suspect Jackson or the client.

(The deeper fix — rejecting a null title at the door — is validation,
tutorial 08.)

## 5. What is deliberately still wrong here

Two smells, each getting its own tutorial:

1. Controllers take and return the `Book` ENTITY. The database shape
   and the API shape are chained together — tutorial 07 (DTOs) breaks
   the chain.
2. Nothing validates input: `{"title":"","price":-5}` would be saved
   happily. Tutorial 08.
3. 404s carry an empty body instead of a JSON error message.
   Tutorial 09.

## 6. Recap

- Verbs do the talking: GET reads, POST creates (201 + Location),
  PUT replaces, DELETE removes (204).
- `@PathVariable` reads the URL; `@RequestBody` reads the body;
  `ResponseEntity` controls the status.
- Service speaks in `Optional` and booleans; controller translates
  to status codes.
- Forgotten `@RequestBody` = all-null object saved with a 201. Silent.

Next: **Tutorial 07 — DTOs and the response envelope**: why entities
must not cross the API boundary, and the
`{success, message, data, meta}` shape every response will use from
now on.

# Tutorial 07 — DTOs and the response envelope

Why entities must not cross the API boundary, records, hand-written
mapping, and the `{success, message, data, meta}` envelope every
endpoint uses from now on.

Files for this stage:
- `dto/ApiResponse.java`, `dto/BookRequest.java`,
  `dto/BookResponse.java` (new)
- `model/Book.java` (+ internal costPrice column)
- `service/BookService.java`, `controller/BookController.java`,
  `controller/ShopController.java` (rewired)

---

## 1. The leak, demonstrated for real

The bookshop buys books cheaper than it sells them. So the entity
gained an internal column:

```java
// What the shop paid for the book. Internal - customers must
// never see the margin.
private BigDecimal costPrice;
```

Nothing else changed — controllers still returned the entity, as
built in tutorial 06. First request after adding the field:

```
GET /api/v1/books/1
{"title":"Effective Java","author":"Joshua Bloch","price":54.99,
 "costPrice":31.00,"id":1}
```

**The margin is public.** Nobody decided to publish it; it leaked
because the API's output shape WAS the database shape. Every column
anyone ever adds — cost price, admin notes, a password hash in
tutorial 15 — ships straight to every client, automatically.

That is the whole argument for DTOs:

> A **DTO** (Data Transfer Object) is a class whose only job is to
> carry data across the API boundary. Inbound and outbound get their
> own DTOs, and the entity stays inside.

```
+----------------------------------+----------------------------------+
| Entity as API shape              | DTOs as API shape                |
+----------------------------------+----------------------------------+
| new column -> instantly public   | new column -> invisible until    |
|                                  | you ADD it to the response DTO   |
| client can try to set any        | client can only send what the    |
| bindable field ("mass            | request DTO declares             |
| assignment")                     |                                  |
| renaming a field silently        | renaming breaks compilation in   |
| renames the JSON for clients     | the mapper - you notice          |
+----------------------------------+----------------------------------+
```

## 2. Records — the right Java for DTOs

> A **record** is Java's short form for an immutable data class:
> `record BookResponse(Long id, String title, ...)` generates the
> fields, constructor, accessors (`id()`, `title()`), `equals`,
> `hashCode`, `toString`. One line instead of forty.

DTOs are dumb data carriers, so records fit perfectly (entities do
NOT get to be records — JPA needs its no-arg constructor and mutable
fields). Jackson understands records natively: components become
JSON fields on the way out, and the canonical constructor is used on
the way in. No getters, no setters, nothing to forget.

The two shapes:

```java
public record BookRequest(String title, String author, BigDecimal price) { }

public record BookResponse(Long id, String title, String author, BigDecimal price) {
    public static BookResponse from(Book book) {
        return new BookResponse(book.getId(), book.getTitle(),
                book.getAuthor(), book.getPrice());
    }
}
```

- `BookRequest` has **no id** (database assigns) and **no costPrice**
  (internal). What a client may send, nothing more.
- `BookResponse` has **no costPrice**. What the API will show,
  nothing more.
- `from(...)` is the mapping: four lines, by hand. (MapStruct can
  generate mappers on big projects; at this size plain code wins.)

Flow: `BookRequest` → controller → service maps to entity → ... →
entity → `BookResponse.from` → out. Entities never appear in a
controller method signature again.

## 3. The envelope

Every response now uses one shape — the same one as the
node-mongo-helper reference, so a client can consume either backend
with the same code:

```json
{ "success": true, "message": "Books fetched", "data": ..., "meta": ... }
```

Implemented as one generic record:

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, String message, T data, Object meta) {
    public static <T> ApiResponse<T> ok(T data) { ... }
    public static <T> ApiResponse<T> ok(String message, T data) { ... }
    public static <T> ApiResponse<T> ok(String message, T data, Object meta) { ... }
}
```

`@JsonInclude(NON_NULL)` keeps null fields out of the JSON — so
`meta` only appears once pagination exists (tutorial 10), and a
data-less response is just `{"success":true,"message":"..."}`.

One deliberate change: DELETE now answers `200` with the envelope
instead of tutorial 06's bare `204`. Both are valid REST; a single
body shape for every endpoint is worth more to API consumers than
the purist no-body delete. (The error side of the envelope —
`"success": false` — is tutorial 09's job; 404s still have empty
bodies today.)

## 4. Verified

```
GET /api/v1/books/1
{"success":true,"message":"Book fetched",
 "data":{"id":1,"title":"Effective Java","author":"Joshua Bloch","price":54.99}}
```

costPrice is gone from the output. And the inbound direction — a
client trying to smuggle fields in:

```
POST /api/v1/books
{"title":"Domain-Driven Design","author":"Eric Evans","price":59.99,
 "costPrice":0.01,"id":77}

HTTP 201
{"success":true,"message":"Book created",
 "data":{"id":4,"title":"Domain-Driven Design","author":"Eric Evans","price":59.99}}
```

`costPrice: 0.01` and `id: 77` were silently dropped — `BookRequest`
has no such components, so there is nothing to bind them to. This is
mass-assignment protection by construction, not by remembering.

```
DELETE /api/v1/books/4  ->  200
{"success":true,"message":"Book deleted"}          (data omitted)

GET /api/v1/shop
{"success":true,"message":"Shop info fetched",
 "data":{"currency":"USD","name":"Bookshop (dev)"}}
```

## 5. The common mistake

The mistake for this topic IS returning entities — and its sneakiest
form is that it works fine for months. The demo above is the honest
version: the day someone adds a sensitive column, every client sees
it, and no error tells you.

The second (coming) form: once entities reference each other
(tutorial 10, `Author` ↔ `Book`), serializing an entity walks the
object graph in circles until the response blows up. DTOs make that
impossible too — `from(...)` copies exactly what you listed, no
graph-walking.

## 6. Recap

- The API's shapes are DTO records: `BookRequest` in (no id, no
  internal fields), `BookResponse` out (only what you chose).
- Records: immutable data classes, one line, Jackson-native. DTOs
  yes, entities no.
- Mapping is four honest lines in `from(...)`.
- Every response wears `{success, message, data, meta}`; null fields
  vanish; matches the Node reference exactly.
- Unknown inbound JSON fields bind to nothing and vanish — by design.

Next: **Tutorial 08 — Validation**: `@Valid`, the constraint
annotations, and stopping `{"title":"","price":-5}` at the door.

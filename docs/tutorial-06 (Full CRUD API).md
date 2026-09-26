# Tutorial 06 — Full CRUD API

Add the full create/read/update/delete API for books.

Files for this stage:

- Updated: `src/main/java/com/example/bookshop/service/BookService.java`
- Updated: `src/main/java/com/example/bookshop/controller/BookController.java`

---

## 1. CRUD map

The usual mapping is:

| HTTP verb | URL                  | Meaning      | Status |
| --------- | -------------------- | ------------ | ------ |
| GET       | `/api/v1/books`      | list books   | 200    |
| GET       | `/api/v1/books/{id}` | get one book | 200    |
| POST      | `/api/v1/books`      | create book  | 201    |
| PUT       | `/api/v1/books/{id}` | update book  | 200    |
| DELETE    | `/api/v1/books/{id}` | delete book  | 204    |

This keeps the URL focused on the resource and the HTTP verb focused on the action.

## 2. Read from the URL with @PathVariable

```java
@GetMapping("/{id}")
public Book getBookById(@PathVariable Long id) {
    return bookService.getBookById(id);
}
```

`{id}` becomes the Java method parameter.

## 3. Read the request body with @RequestBody

```java
@PostMapping
public ResponseEntity<Book> createBook(@RequestBody Book book) {
    Book saved = bookService.save(book);
    return ResponseEntity.created(URI.create("/api/v1/books/" + saved.getId())).body(saved);
}
```

This is how JSON sent by the client turns into a Java object.

## 4. Use ResponseEntity for status and headers

```java
return ResponseEntity.created(URI.create("/api/v1/books/" + saved.getId())).body(saved);
```

This returns:

- status 201
- `Location` header
- created body

For delete:

```java
return ResponseEntity.noContent().build();
```

## 5. Common mistake: forgetting @RequestBody

If you forget `@RequestBody`, Spring will not read the JSON body correctly.
The object may come through as empty or null-valued, and the API may still return 201.

This is a very common bug in CRUD tutorials.

## 6. Keep the service responsible for logic

The controller should mostly do:

- receive request
- pass it to service
- translate result to HTTP status and response

The service should do the actual business work.

## 7. Goal for this tutorial

By the end of this tutorial, you should know:

- how to map CRUD routes
- how `@PathVariable` and `@RequestBody` work
- how `ResponseEntity` controls HTTP status
- why the API should use verbs and resources correctly

Next: [**Tutorial 07 — DTOs and the response envelope**](tutorial-07%20%28DTOs%20and%20the%20response%20envelope%29.md)

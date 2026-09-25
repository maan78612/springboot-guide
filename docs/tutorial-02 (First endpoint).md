# Tutorial 02 — First endpoint

@RestController, @GetMapping, how a Java object becomes JSON, and the
/api/v1 prefix.

Files for this stage:
- `src/main/java/com/example/bookshop/model/Book.java`
- `src/main/java/com/example/bookshop/controller/BookController.java`

---

## 1. Why: what an endpoint is

> An **endpoint** is one URL + one HTTP method that your API answers.
> `GET /api/v1/books` ("give me the books") is an endpoint.
> `POST /api/v1/books` ("create a book") would be a different one,
> even though the URL is the same.

In tutorial 1 every request got a 404, because the server was running
but we had registered nothing. Registering an endpoint in Spring means:
write a normal method, and put annotations on it that describe which
requests it answers. You never call the method — Spring does, when a
matching request arrives. (The restaurant kitchen calling the chef.)

## 2. The two new files

**`model/Book.java`** — a plain Java class: four private fields
(`id`, `title`, `author`, `price`), a constructor, four getters.
Nothing from Spring in it:

```java
package com.example.bookshop.model;

import java.math.BigDecimal;

public class Book {

    private Long id;
    private String title;
    private String author;
    private BigDecimal price;

    public Book(Long id, String title, String author, BigDecimal price) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.price = price;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
```

Two choices worth explaining:

- `price` is a `BigDecimal`, not `double`. Binary floating point
  cannot store `0.10` exactly, and money errors compound. Money is
  always `BigDecimal` (or whole cents in a `long`), never `double`.
- `author` is plain text for now. It becomes a real `Author` object
  with its own table in tutorial 10.

**`controller/BookController.java`** — the endpoint:

```java
@RestController
@RequestMapping("/api/v1/books")
public class BookController {

    @GetMapping
    public List<Book> getAllBooks() {
        return List.of(
                new Book(1L, "Effective Java", "Joshua Bloch", new BigDecimal("54.99")),
                ...);
    }
}
```

The three annotations:

```
+----------------------------+-------------------------------------------+
| Annotation                 | What it tells Spring                      |
+----------------------------+-------------------------------------------+
| @RestController            | Create one instance of this class at      |
|                            | startup and manage it. Whatever its       |
|                            | methods return IS the response body       |
|                            | (JSON) - not the name of an HTML page.    |
| @RequestMapping("/api/     | Every method's URL starts with this       |
|   v1/books")               | prefix.                                   |
| @GetMapping                | Run this method for GET requests to that  |
|                            | URL. (@PostMapping etc. exist - tut. 06)  |
+----------------------------+-------------------------------------------+
```

In simple words:

- **`@RestController`** — "This class handles web requests." Spring
  creates the object for you (you never write `new BookController()`).
  Whatever a method returns is sent back to the caller as JSON.
  Return a `List<Book>`, and the caller gets a JSON array.
- **`@RequestMapping("/api/v1/books")`** — the base URL for the whole
  class. Every endpoint inside starts with `/api/v1/books`, so you
  don't repeat it on each method.
- **`@GetMapping`** — "Call this method when someone sends a GET
  request to that URL." GET means "give me data." Other annotations
  like `@PostMapping` handle other request types, such as creating
  data.

If you know Express in JS, it's the same idea:

```js
const router = express.Router();          // @RestController
app.use('/api/v1/books', router);         // @RequestMapping("/api/v1/books")

router.get('/', (req, res) => {           // @GetMapping
  res.json(books);                        // return value → JSON
});
```

The difference: in Express you wire it up yourself in code. In Spring
you put annotations on the class and method, and Spring does the
wiring.

Why the `/api/v1` prefix on everything, forever:

- `/api` separates the JSON API from other URLs (health checks, docs).
- `/v1` is insurance: when a breaking change is unavoidable someday,
  `/v2` can exist while old clients keep working on `/v1`.

## 3. Run and test it

```bash
./mvnw spring-boot:run
```

Then in a second terminal:

```bash
curl -i http://localhost:8080/api/v1/books
```

Real output:

```
HTTP/1.1 200
Content-Type: application/json

[{"id":1,"title":"Effective Java","author":"Joshua Bloch","price":54.99},
 {"id":2,"title":"Clean Code","author":"Robert C. Martin","price":42.50},
 {"id":3,"title":"The Pragmatic Programmer","author":"Andrew Hunt","price":49.95}]
```

Anatomy of the URL you just called:

```
http://localhost:8080/api/v1/books
+----+ +-------+ +--+ +----------+
|      |         |    |
|      |         |    the path. Spring matches it to a method.
|      |         the port Tomcat listens on (tutorial 4 changes it)
|      "this same machine"
the protocol
```

## 4. What happened to that object on the way out

Your method returned `List<Book>` — Java objects on the heap. curl
received text. The converter in between is **Jackson**, a library the
web starter brought along. For each object it:

1. takes the object your method returned
2. finds its **public getters** (`getId`, `getTitle`, ...)
3. writes one JSON field per getter: `getTitle()` → `"title": ...`
4. strips the `get` prefix and lowercases the first letter

So JSON is built from **getters, not fields**. The fields are private;
Jackson never sees them (by default). This has a sharp edge, which is
this tutorial's common mistake.

## 5. The common mistake — no public getters

I removed `public` from the four getters and called the endpoint
again. What actually happened:

```
HTTP 200
[{},{},{}]
```

Three books, all empty. **No error, no log line, HTTP 200.** Jackson
found zero public getters, so each Book produced `{}`. This is the
sneakiest failure so far: everything looks fine except the data is
gone. Older Spring Boot versions threw a 500 error here; Boot 4 fails
silently.

If your JSON is missing one field (not all): check that specific
getter — it is misspelled (`gettitle`), non-public, or absent.

Second, smaller mistake: calling `/books` instead of `/api/v1/books`
gets the same 404 you saw in tutorial 1. The path must match the
`@RequestMapping` prefix + `@GetMapping` exactly.

## 6. Recap

- One endpoint = one URL + one method, declared with annotations on a
  normal Java method that Spring calls for you.
- `@RestController` means "return values are the JSON body".
- Jackson converts objects to JSON using public getters. No getters →
  silent `{}`.
- Money is `BigDecimal`. Always.
- The hardcoded list inside the controller is deliberately wrong —
  a controller should hold no data. Next tutorial fixes exactly that.

Next: [**Tutorial 03 — Layers and dependency injection.**](tutorial-03%20%28Layers%20and%20dependency%20injection%29.md)

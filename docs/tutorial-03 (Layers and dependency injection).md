# Tutorial 03 — Layers and dependency injection

controller / service / repository, beans, the container, constructor
injection, and why we never write `new` for our own layers.

Files for this stage:

- `repository/BookRepository.java` (new)
- `service/BookService.java` (new)
- `controller/BookController.java` (changed — the list moved out)

The endpoint behaves exactly as before. This whole tutorial changes
_structure_, not behavior. That is on purpose.

---

## 1. Why layers

Tutorial 02 ended with data hardcoded inside the controller. With ten
endpoints that style becomes a controller that parses HTTP, enforces
business rules, AND talks to storage — three reasons to change, one
class. You know this smell from SOLID: single responsibility.

The standard backend answer (your Node repo uses the same one:
routes → controllers → services → models) is three layers:

```
+-------------+----------------------------------------------------+
| Layer       | Its ONE job                                        |
+-------------+----------------------------------------------------+
| controller  | Speak HTTP. Read the request, call the service,    |
|             | choose the status code. Nothing else.              |
| service     | Business rules. "What does the bookshop DO."      |
| repository  | Store and load data. Nothing else.                 |
+-------------+----------------------------------------------------+
```

**Analogy.** Controller = waiter (talks to customers, carries orders,
knows nothing about cooking). Service = kitchen (makes the real
decisions). Repository = pantry (fetches ingredients; the kitchen
does not care which supplier filled the shelves).

Requests flow one way: controller → service → repository. A
controller never touches a repository directly, and lower layers
never know the ones above them exist.

Right now `BookService.getAllBooks()` just forwards one call, which
looks pointless. The payoff comes soon: rules, transactions and
ownership checks all land in services — and in tutorial 05 we swap
the repository's insides for a real database and the service does not
change by one character.

## 2. The repository layer: what it is and why it exists

A repository is the layer that knows how to load and save data. It is
not the database itself, and it is not the controller. It is the
boundary between the business code and storage.

A very small, concept-only version looks like this:

```java
package com.example.bookshop.repository;

import java.util.List;

import com.example.bookshop.model.Book;

public interface BookRepository {
    List<Book> getAllBooks();
}
```

Then the service depends on that interface instead of constructing
anything itself:

```java
@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<Book> getAllBooks() {
        return bookRepository.getAllBooks();
    }
}
```

And the controller simply asks the service for the result:

```java
@RestController
@RequestMapping("/api/v1/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public List<Book> getAllBooks() {
        return bookService.getAllBooks();
    }
}
```

The important idea is: the service depends on a repository
abstraction, not on a concrete storage implementation. Later, that
repository can be backed by a HashMap, a database, or a Spring Data
JPA implementation, and the service still does not change.

This is the whole point of layers: the business logic stays stable
while the storage mechanism can be swapped underneath it.

In the real project, the repository eventually becomes a Spring Data
JPA interface, which is why the file in `repository/BookRepository.java`
looks larger and more advanced than this minimal example. The idea is
the same; only the implementation detail becomes more powerful.

## 3. Beans and the container

> A **bean** is an object that Spring creates and manages for you.
> The **container** (also "application context") is the registry
> that holds all beans.

Remember tutorial 1: at startup, `@SpringBootApplication` scans
`com.example.bookshop` and below. Every class marked with
`@Component` — or one of its role-specific aliases — becomes a bean:

```
+-----------------+---------------------------------------+
| Annotation      | Meaning                               |
+-----------------+---------------------------------------+
| @Component      | generic "manage this class for me"    |
| @Repository     | @Component that stores/loads data     |
| @Service        | @Component that holds business logic  |
| @RestController | @Component that answers HTTP          |
+-----------------+---------------------------------------+
```

The last three ARE `@Component` underneath. The different names
document intent (and unlock a few layer-specific extras later).

One instance each: beans are **singletons** by default. Everyone who
asks for `BookRepository` gets the same object. That's why its
`books` map must not be `static` — the bean is already
one-per-application.

## 3. Dependency injection

> **Dependency injection (DI):** a class does not build what it
> needs. It declares needs as constructor parameters, and the
> container hands them in.

The whole pattern is three lines, repeated in service and controller:

```java
@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }
    ...
}
```

While creating the `BookService` bean, Spring sees the constructor
needs a `BookRepository`, finds that bean in the container, and
passes it in. Order is worked out automatically: repository first,
then service, then controller.

**Why not `new BookRepository()` inside the service?**

1. **Swap.** Tutorial 05 replaces the HashMap repository with a
   database one. Because the service only _receives_ a repository,
   it will not change at all.
2. **Tests.** A test can construct `BookService` with a fake
   repository (tutorial 13). Impossible with a hard-wired `new`.
3. **Sharing.** Beans are singletons. `new` creates private copies
   with separate state — two copies of the book map would disagree.

`new` is still fine for plain values (`new Book(...)`,
`new BigDecimal(...)`). The rule of thumb: **layers are injected,
data is `new`ed.**

**Constructor injection specifics:**

- The field is `final`, so the service can never exist half-built,
  and its dependencies are visible in one place: the constructor.
- No `@Autowired` needed — with exactly one constructor, Spring uses
  it automatically.
- Old tutorials show `@Autowired` on fields. That works but hides
  dependencies, blocks `final`, and makes plain-Java testing painful.
  Prefer the constructor. Always.

## 4. Verify it

With the app running (`./mvnw spring-boot:run`):

```bash
curl -s -w "\nHTTP %{http_code}\n" http://localhost:8080/api/v1/books
```

Real output — identical to tutorial 02, which is the point:

```
[{"id":1,"title":"Effective Java","author":"Joshua Bloch","price":54.99},
 {"id":2,"title":"Clean Code","author":"Robert C. Martin","price":42.50},
 {"id":3,"title":"The Pragmatic Programmer","author":"Andrew Hunt","price":49.95}]
HTTP 200
```

## 5. The common mistake — asking for something that is not a bean

I deleted `@Repository` from `BookRepository` and recompiled. The
app failed to start with:

```
***************************
APPLICATION FAILED TO START
***************************

Description:

Parameter 0 of constructor in com.example.bookshop.service.BookService
required a bean of type 'com.example.bookshop.repository.BookRepository'
that could not be found.

Action:

Consider defining a bean of type
'com.example.bookshop.repository.BookRepository' in your configuration.
```

Read it slowly once and it is actually a good message: _BookService's
constructor wanted a BookRepository and the container had none._ The
two causes you will actually hit:

1. The class is missing `@Repository`/`@Service`/`@Component`
   (this repro).
2. The class lives OUTSIDE the root package, so component scan never
   saw it (the warning from tutorial 01).

Bonus fact, verified while doing this: devtools kept the broken app's
process alive, and after I put `@Repository` back and recompiled, it
restarted successfully on its own. You do not have to relaunch after
a failed hot reload — just fix and save.

## 6. Recap

- Three layers, one job each; requests flow controller → service →
  repository, never sideways or upward.
- A bean = an object Spring builds and keeps; component scan +
  `@Component`-family annotations decide what becomes one.
- Declare dependencies as `final` constructor parameters and let the
  container wire them. `new` layers nowhere; `new` data anywhere.
- "required a bean ... could not be found" = the class you want
  isn't a bean (missing annotation or outside the scanned package).

Next: [**Tutorial 04 — Configuration**](tutorial-04%20%28Configuration%29.md): application.properties,
profiles, @Value, @ConfigurationProperties, and where secrets do NOT
go.

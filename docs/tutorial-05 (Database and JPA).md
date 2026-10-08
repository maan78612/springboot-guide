# Tutorial 05 — Database and JPA

Move the app from in-memory lists to a real database-backed JPA layer.

Files for this stage:

- Updated: `pom.xml`
- Updated: `src/main/java/com/example/bookshop/model/Book.java`
- New: `src/main/java/com/example/bookshop/repository/BookRepository.java`
- New: `src/main/resources/data.sql`
- Updated: `src/main/resources/application.properties`
- Updated: `src/main/resources/application-dev.properties`

---

## 1. Add JPA and H2

Add Spring Data JPA and H2 to the project:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Required by Spring Boot 4 to serve the H2 browser console -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-h2console</artifactId>
    <scope>runtime</scope>
</dependency>
```

This gives you:

- JPA mapping support
- database repository generation
- an in-memory H2 database for development
- the H2 Console web page

## 2. Turn Book into an entity

Add JPA annotations to `Book.java`. The protected no-argument constructor is required by Hibernate to instantiate entities via reflection. Keep all public getters so Jackson can serialize the entity to JSON:

```java
package com.example.bookshop.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * JPA mapping from the Book class to a database row.
 *
 * | Key / Annotation               | Why we use it                                  |
 * |--------------------------------|------------------------------------------------|
 * | @Entity                        | Marks Book as a persisted database entity      |
 * | @Id                            | Identifies the primary key                     |
 * | @GeneratedValue(IDENTITY)      | Lets the database generate the id              |
 * | @Column(precision=10, scale=2) | Stores the price with two decimal places       |
 * | protected no-arg constructor   | Allows JPA to instantiate rows from the database|
 */
@Entity
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String author;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    protected Book() {
    }

    public Book(String title, String author, BigDecimal price) {
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

Important parts:

- `@Entity` → map this class to a database table named `book`
- `@Id` → primary key
- `@GeneratedValue(strategy = GenerationType.IDENTITY)` → database auto-increments the ID
- protected no-arg constructor → required by JPA/Hibernate proxy mechanism
- public getters → required by Jackson for JSON serialization

## 3. Replace the manual repository with Spring Data JPA

Create/replace `BookRepository.java` as an interface extending `JpaRepository`:

```java
package com.example.bookshop.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bookshop.model.Book;

/**
 * Spring Data creates this repository implementation at startup.
 *
 * | Key                       | Why we use it                                  |
 * |---------------------------|------------------------------------------------|
 * | JpaRepository<Book, Long> | Supplies CRUD methods for Book and its Long id |
 */
public interface BookRepository extends JpaRepository<Book, Long> {
}
```


This gives you methods such as:

- `findAll()`
- `findById()`
- `save()`
- `deleteById()`

Spring generates the implementation for you at startup.

### Update the service from tutorial 3

In tutorial 3, your handmade repository declared `getAllBooks()`. Replace that repository with the JPA interface above, then update the service to use the built-in `findAll()` method:

```java
public List<Book> getAllBooks() {
    return bookRepository.findAll();
}
```

Keep the service method named `getAllBooks()`; only change the call on `bookRepository`. `findAll()` is already provided by `JpaRepository`, so do not declare `getAllBooks()` in `BookRepository`.

If the app fails at startup with `No property 'getAllBooks' found for type 'Book'`, Spring Data is trying to build a database query from a repository method named `getAllBooks()`. Remove that method from the repository and call `findAll()` from the service as shown above. Spring Data derives query methods from names such as `findByTitle(...)`; `getAllBooks()` is not a built-in repository method.

## 4. Seed the database

Create `data.sql`:

```sql
INSERT INTO book (title, author, price) VALUES
  ('Effective Java', 'Joshua Bloch', 54.99),
  ('Clean Code', 'Robert C. Martin', 42.50),
  ('The Pragmatic Programmer', 'Andrew Hunt', 49.95);
```

Also enable deferred initialization:

```properties
spring.jpa.defer-datasource-initialization=true
```

This ensures the table exists before the insert runs.

## 5. View the SQL

In dev config:

```properties
spring.jpa.show-sql=true
```

Now when you call the API, you can see the generated SQL in the console.

## 6. Use the H2 console

In dev config:

```properties
spring.h2.console.enabled=true
```

Start the app with the `dev` profile so Spring loads `application-dev.properties`:

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

Then open this address in your browser:

```text
http://localhost:8080/h2-console
```

Use:

- JDBC URL: `jdbc:h2:mem:bookshop`
- username: `sa`
- password: empty

This lets you inspect the database directly.

If this URL returns 404 with `No static resource h2-console`, check that the `spring-boot-h2console` dependency is in `pom.xml`, that the `dev` profile is active, and that the app was restarted after adding the dependency. A 404 means the console page was not registered; it is different from an H2 login failure.

## 7. Verify the database endpoint

With the app running under the `dev` profile:

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

Call the books endpoint:

```bash
curl http://localhost:8080/api/v1/books
```

You should receive the seeded rows from `data.sql`:

```json
[
  {
    "id": 1,
    "title": "Effective Java",
    "author": "Joshua Bloch",
    "price": 54.99
  },
  {
    "id": 2,
    "title": "Clean Code",
    "author": "Robert C. Martin",
    "price": 42.5
  },
  {
    "id": 3,
    "title": "The Pragmatic Programmer",
    "author": "Andrew Hunt",
    "price": 49.95
  }
]
```

### JPA Architecture Overview

```mermaid
flowchart LR
    Service["BookService"] -->|Calls findAll()| Repo["BookRepository<br/>(Spring Data Proxy)"]
    Repo -->|Executes JPQL / SQL| Hibernate["Hibernate ORM Engine<br/>(JPA Provider)"]
    Hibernate -->|JDBC Connection| H2["In-Memory H2 DB<br/>(jdbc:h2:mem:bookshop)"]
```

## 8. Common mistakes

- **Forgetting `@Id` on the entity**: JPA entities must have a primary key field marked with `@Id`.
- **Forgetting the no-arg constructor**: Hibernate uses reflection to construct empty entity objects before populating fields from the SQL ResultSet.
- **Forgetting `spring.jpa.defer-datasource-initialization=true`**: Without this setting, Spring Boot attempts to execute `data.sql` before Hibernate auto-generates the database schema tables, causing an `EmbeddedDatabaseException: Table 'BOOK' not found`.
- **Declaring custom methods named `getAllBooks()` in `JpaRepository`**: Spring Data interprets method names as query creators. Unless you write `@Query`, non-standard method names cause a startup failure (`No property 'getAllBooks' found for type 'Book'`). Use the built-in `findAll()` method instead.

## 9. Goal for this tutorial

By the end of this tutorial, you should understand:

- why JPA maps Java classes to database tables
- what `JpaRepository` provides out of the box
- how an in-memory H2 database runs during development
- how to inspect runtime database tables and SQL queries via the H2 Console

Next: [**Tutorial 06 — Full CRUD API**](tutorial-06%20%28Full%20CRUD%20API%29.md)


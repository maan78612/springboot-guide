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
```

This gives you:

- JPA mapping support
- database repository generation
- an in-memory H2 database for development

## 2. Turn Book into an entity

```java
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
}
```

Important parts:

- `@Entity` → map this class to a database table
- `@Id` → primary key
- `@GeneratedValue` → database generates the id
- no-arg constructor → required by JPA

## 3. Replace the manual repository with Spring Data JPA

```java
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

Then open:

```text
http://localhost:8080/h2-console
```

Use:

- JDBC URL: `jdbc:h2:mem:bookshop`
- username: `sa`
- password: empty

This lets you inspect the database directly.

## 7. Common mistakes

- forgetting `@Id` on the entity
- forgetting the no-arg constructor
- forgetting `spring.jpa.defer-datasource-initialization=true`

## 8. Goal for this tutorial

By the end of this tutorial, you should understand:

- why JPA maps Java classes to database tables
- what `JpaRepository` gives you
- how H2 is used for local development
- how SQL appears in the logs

Next: [**Tutorial 06 — Full CRUD API**](tutorial-06%20%28Full%20CRUD%20API%29.md)

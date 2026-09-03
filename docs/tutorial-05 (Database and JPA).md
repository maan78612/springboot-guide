# Tutorial 05 — Database and JPA

H2, @Entity, @Id, JpaRepository, what Spring generates for you, and
watching the SQL it runs.

Files for this stage:
- `pom.xml` (+ data-jpa starter, + h2, + spring-boot-h2console)
- `model/Book.java` (became an @Entity)
- `repository/BookRepository.java` (class → one-line interface)
- `src/main/resources/data.sql` (new — seed rows)
- `application.properties` and `application-dev.properties` (grew)

`BookService` and `BookController` did not change by one character.
That is tutorial 03 paying off.

---

## 1. The names, untangled first

Four names show up around Java persistence and beginners mix them up:

```
+-------------+------------------------------------------------------+
| JPA         | A STANDARD (just interfaces/annotations): how Java   |
|             | objects map to database rows. Like an interface.     |
| Hibernate   | The most-used IMPLEMENTATION of JPA. Does the real   |
|             | work: generates SQL, tracks objects. Like the class. |
| Spring Data | A Spring layer ON TOP of JPA that writes the         |
| JPA         | repository code for you.                             |
| H2          | A tiny database written in Java that can live        |
|             | IN MEMORY inside your app. Zero install.             |
+-------------+------------------------------------------------------+
```

**Why H2 first instead of a real database:** it needs no
installation, starts empty in milliseconds, and vanishes on shutdown.
It is the Spring twin of your Node repo's `npm run dev:memory`
(mongodb-memory-server): perfect for learning and tests, never for
production. Postgres arrives in tutorial 18; today nothing needs
installing.

You wrote JDBC by hand in your Java course: SQL strings,
ResultSet loops, copying columns into objects. JPA's pitch is that
the mapping (`row ↔ object`) is declared ONCE, on the class, and the
SQL is generated.

## 2. Book becomes an @Entity

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

    protected Book() { }          // for JPA only

    public Book(String title, String author, BigDecimal price) { ... }
```

- `@Entity` — one object of this class = one row in a table.
  Hibernate maps names automatically: class `Book` → table `book`,
  field `title` → column `title` (camelCase → snake_case).
- `@Id` — this field is the primary key.
- `@GeneratedValue(IDENTITY)` — the DATABASE assigns ids on insert
  (1, 2, 3...). Code never sets ids; note `id` has no setter and the
  public constructor takes no id.
- `protected Book() {}` — JPA builds objects empty via reflection,
  then fills the fields from the row. It needs a no-arg constructor.
  `protected` keeps other code from creating half-empty books.
- `@Column(precision = 10, scale = 2)` — money: 10 digits total, 2
  after the point.

Proof of the mapping — this line appeared in the log at startup
(Hibernate created the schema from the entity):

```
Hibernate: create table book (price numeric(10,2), id bigint generated
  by default as identity, author varchar(255), title varchar(255),
  primary key (id))
```

## 3. The repository shrinks to one line

The HashMap class from tutorial 03 is gone. The whole file is now:

```java
public interface BookRepository extends JpaRepository<Book, Long> {
}
```

An interface with an EMPTY body — so where is the code? At startup,
Spring Data sees the interface, reads the type arguments (entity
`Book`, id type `Long`), and generates the implementing class at
runtime. That generated object is the bean your service injects.

Inherited for free: `findAll()`, `findById(id)`, `save(entity)`,
`deleteById(id)`, `count()`, `existsById(id)`, and more — each one
turned into real SQL when called. Tutorial 06 uses most of them.

## 4. Seed data: data.sql

An in-memory database starts empty on every run. Spring runs
`src/main/resources/data.sql` automatically at startup:

```sql
INSERT INTO book (title, author, price) VALUES
  ('Effective Java', 'Joshua Bloch', 54.99),
  ...
```

Two deliberate choices in it:

1. **No ids in the INSERT.** The identity column assigns 1, 2, 3. If
   we inserted explicit ids, the identity counter would still be at
   1, and the first API-created book would collide with a seeded id.
2. One line in `application.properties` makes the timing work:
   `spring.jpa.defer-datasource-initialization=true` — Hibernate must
   create the table BEFORE data.sql inserts into it. Without the
   line, boot fails with "Table BOOK not found".

(data.sql is the H2-phase solution. Tutorial 18 replaces it with
Flyway migrations, the production tool.)

## 5. Seeing the SQL

In `application-dev.properties`:

```properties
spring.jpa.show-sql=true
```

Run with the dev profile and call the endpoint:

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
curl http://localhost:8080/api/v1/books
```

Real output — the same three books, now coming out of H2:

```
[{"title":"Effective Java","author":"Joshua Bloch","price":54.99,"id":1},
 {"title":"Clean Code","author":"Robert C. Martin","price":42.50,"id":2},
 {"title":"The Pragmatic Programmer","author":"Andrew Hunt","price":49.95,"id":3}]
```

And in the app's log, the SQL that `findAll()` became:

```
Hibernate: select b1_0.id,b1_0.author,b1_0.price,b1_0.title from book b1_0
```

Keep show-sql on for the whole course. Reading the generated SQL is
how you catch JPA doing something dumb (tutorial 10's N+1 problem is
exactly that, and this log line is how we will SEE it).

## 6. Looking inside the database: the H2 console

`spring.h2.console.enabled=true` (dev profile) serves a small
database UI from your own app. Boot 4 note: the console moved to its
own module, so the pom needs `spring-boot-h2console` — without it the
property is silently ignored and `/h2-console` 404s (I hit exactly
that building this).

Verified — startup log:

```
H2 console available at '/h2-console'. Database available at 'jdbc:h2:mem:bookshop'
```

Open http://localhost:8080/h2-console in a browser and log in with:

```
JDBC URL:  jdbc:h2:mem:bookshop
User:      sa
Password:  (leave empty)
```

Then run `SELECT * FROM book;` and you are looking at your API's
data. (The fixed name `jdbc:h2:mem:bookshop` comes from
`spring.datasource.url` in the dev profile — otherwise H2 picks a
random name per start and you'd have to fish it out of the log.)

Where did the schema come from? For an embedded database Spring Boot
defaults to `ddl-auto=create-drop`: Hibernate drops and recreates
tables from the entities on every start. Convenient now; totally
wrong for production — tutorial 18 turns it off in favor of
migrations.

## 7. The common mistakes (both reproduced)

**Forgot `@Id`** — fails fast at startup, clear message:

```
Entity 'com.example.bookshop.model.Book' has no identifier (every
'@Entity' class must declare or inherit at least one '@Id' or
'@EmbeddedId' property)
```

**Forgot the no-arg constructor** — the nasty one. The app starts
WITHOUT any complaint, and then the first read explodes:

```
GET /api/v1/books  ->  HTTP 500
"message": "No default constructor for entity
            'com.example.bookshop.model.Book'"
```

A bug that hides until the first query is worse than one that kills
startup. When an entity 500s on read, check for the no-arg
constructor first. (You will meet this the day you add a custom
constructor to an entity — Java then stops generating the default
one.)

## 8. Recap

- JPA = the standard, Hibernate = the engine, Spring Data = writes
  the repository, H2 = throwaway in-memory database for dev.
- `@Entity` + `@Id` + `@GeneratedValue` map class ↔ table; the
  database owns ids; entities need a no-arg constructor.
- `JpaRepository<Book, Long>` gives CRUD methods with zero code.
- `data.sql` seeds; `defer-datasource-initialization` fixes the
  timing; never insert explicit ids into an identity column.
- `show-sql` + the H2 console let you SEE what JPA actually does.
  Trust the log, not your assumption.

Next: **Tutorial 06 — A full CRUD API**: POST, PUT, DELETE,
@PathVariable, @RequestBody, and the right status code for each case.

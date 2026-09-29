# Tutorial 10 — Relationships and queries

Replace text-only book authors with database relationships, then search and page through the catalog.

Files for this stage:

- New: `src/main/java/com/example/bookshop/model/Author.java`
- New: `src/main/java/com/example/bookshop/model/Genre.java`
- Updated: `src/main/java/com/example/bookshop/model/Book.java`
- New: `src/main/java/com/example/bookshop/repository/AuthorRepository.java`
- New: `src/main/java/com/example/bookshop/repository/GenreRepository.java`
- Updated: `src/main/java/com/example/bookshop/repository/BookRepository.java`
- New: `src/main/java/com/example/bookshop/service/AuthorService.java`
- Updated: `src/main/java/com/example/bookshop/service/BookService.java`
- New: `src/main/java/com/example/bookshop/dto/AuthorResponse.java`
- New: `src/main/java/com/example/bookshop/dto/PageMeta.java`
- Updated: `src/main/java/com/example/bookshop/dto/BookRequest.java`
- Updated: `src/main/java/com/example/bookshop/dto/BookResponse.java`
- New: `src/main/java/com/example/bookshop/controller/AuthorController.java`
- Updated: `src/main/java/com/example/bookshop/controller/BookController.java`
- Updated: `src/main/resources/data.sql`

These examples build on the version from tutorials 7-9. Tutorial 18 later replaces `data.sql` with Flyway migrations.

---

## 1. Create the Author and Genre entities

Create `Author.java`. JPA reads this relationship in two steps: `List<Book>` tells it to look at the `Book` entity, and `mappedBy = "author"` tells it to use the `author` field inside `Book`. Together, that means `Book["author"]`; the type identifies the class, while the string is a field name. If you changed the collection to `List<Genre>`, JPA would instead look for `Genre.author` and fail at startup if that field does not exist.

```java
package com.example.bookshop.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

/**
 * Author entity with the inverse side of the book relationship.
 *
 * | Key                    | Why we use it                                      |
 * |------------------------|----------------------------------------------------|
 * | @Entity                | Maps authors to database rows                     |
 * | @Id / @GeneratedValue  | Marks the database-generated identifier             |
 * | @OneToMany             | Represents one author having many books             |
 * | mappedBy = "author"   | Points to Book.author, the owning foreign-key side  |
 * | protected constructor  | Allows JPA to create an entity from a database row |
 */
@Entity
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    // JPA reads this in two steps: List<Book> selects the Book entity, then
    // mappedBy = "author" selects the field named author inside Book (Book.author).
    // Think Book["author"]: the type picks the class, and the string is its field name.
    // If Book has no author field, JPA fails while validating the mapping at startup.
    @OneToMany(mappedBy = "author")
    private List<Book> books = new ArrayList<>();

    protected Author() {
    }

    public Author(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Book> getBooks() {
        return books;
    }
}
```

Create `Genre.java`. This app only needs to look up and return genre names, so it does not need a reverse collection of books:

```java
package com.example.bookshop.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Entity for a catalog genre.
 *
 * | Key                   | Why we use it                                   |
 * |-----------------------|-------------------------------------------------|
 * | @Entity               | Maps Genre objects to rows in the genre table   |
 * | @Id / @GeneratedValue | Marks the generated primary key                |
 * | no reverse collection | Only maps relationships the app needs to use   |
 */
@Entity
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    protected Genre() {
    }

    public Genre(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
```

## 2. Replace the author text field in `Book`

In `Book.java`, remove `private String author;` and its old string constructor argument. Add these imports and fields instead:

```java
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.BatchSize;

import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
```

The `Author` type on the field identifies the target entity, and `@ManyToOne` says many books may reference the same author. This side owns the relationship. `@JoinColumn(name = "author_id")` names the database foreign-key column on the book row; it is different from `mappedBy = "author"`, which names the Java field `Book.author`. `optional = false` requires an author, while `FetchType.LAZY` defers loading it until accessed.

```java
/**
 * Relationship mapping added to Book.
 *
 * | Key / Annotation          | Why we use it                                  |
 * |---------------------------|------------------------------------------------|
 * | @ManyToOne                 | Many books can reference one author            |
 * | FetchType.LAZY             | Loads the author when the application accesses it|
 * | optional = false           | Requires each book to have an author           |
 * | @JoinColumn(author_id)     | Stores the author foreign key on the book row  |
 * | @ManyToMany                | Allows each book to have multiple genres       |
 * | @BatchSize(50)             | Batches lazy genre loading for up to 50 books  |
 * | @JoinTable(book_genre)     | Stores book/genre links in a separate table    |
 * | joinColumns                | Names the link table column for the Book side  |
 * | inverseJoinColumns        | Names the link table column for the Genre side |
 * | Set / HashSet              | Avoids duplicate genres and starts non-null    |
 */
// The Author field type tells JPA which entity this relationship targets.
// @ManyToOne means many books can reference one author; this is the owning side.
// @JoinColumn names the database foreign-key column on the book row: author_id.
// Author.books uses mappedBy = "author" to point back to this Java field, not that column.
// optional = false requires an author; LAZY defers loading it until accessed.
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "author_id")
private Author author;

// A book can have many genres, and each genre can belong to many books.
// LAZY loads this collection when accessed instead of with every book query.
@ManyToMany(fetch = FetchType.LAZY)
// When loading genres for books in a batch, Hibernate can fetch for up to 50 books together.
@BatchSize(size = 50)
// Stores the many-to-many pairs in a separate join table.
@JoinTable(
        name = "book_genre",
        // The book_id column points back to the owning Book row.
        joinColumns = @JoinColumn(name = "book_id"),
        // The genre_id column points to the related Genre row.
        inverseJoinColumns = @JoinColumn(name = "genre_id"))
// A Set prevents duplicate genres for one book; genre order is not stored.
private Set<Genre> genres = new HashSet<>();
```

The book side owns both mappings: `author_id` is stored on the book row, and `book_genre` stores book/genre pairs. `Author.books` is the inverse side because it uses `mappedBy = "author"`.

Update the constructor and accessors to use `Author` and expose the genre set:

```java
public Book(String title, Author author, BigDecimal price) {
    this.title = title;
    this.author = author;
    this.price = price;
}

public Author getAuthor() {
    return author;
}

public void setAuthor(Author author) {
    this.author = author;
}

public Set<Genre> getGenres() {
    return genres;
}

public void setGenres(Set<Genre> genres) {
    this.genres = genres;
}
```

Keep the other fields, JPA annotations, constructor, and methods already in `Book`. `FetchType.LAZY` means related data is loaded when needed instead of automatically loading every relationship for every book. `@BatchSize` lets Hibernate load genres for several books together when the response mapper touches them, avoiding one genre query per book in a page.

## 3. Change the request and response DTOs

Update `BookRequest.java`: clients now send an existing author's id and optional genre ids, not nested entity objects. Keep the validation annotations from tutorial 8.

```java
package com.example.bookshop.dto;

import java.math.BigDecimal;
import java.util.Set;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record BookRequest(
        @NotBlank(message = "title is required")
        @Size(max = 200, message = "title must be at most 200 characters")
        String title,

        @NotNull(message = "authorId is required")
        @Positive(message = "authorId must be a positive number")
        Long authorId,

        Set<Long> genreIds,

        @NotNull(message = "price is required")
        @Positive(message = "price must be greater than 0")
        @Digits(integer = 8, fraction = 2, message = "price must have at most 2 decimal places")
        BigDecimal price) {
}
```

| Key / Constraint | Why we use it                                    |
| ---------------- | ------------------------------------------------ |
| authorId         | References an existing author without nesting it |
| genreIds         | Optionally references existing genres            |
| @NotNull         | Requires an author id and price                  |
| @Positive        | Rejects zero or negative ids/prices              |

Update `BookResponse.java` to return an author summary and genre names, not the full entity graph:

```java
package com.example.bookshop.dto;

import java.math.BigDecimal;
import java.util.List;

import com.example.bookshop.model.Book;
import com.example.bookshop.model.Genre;

public record BookResponse(Long id, String title, AuthorSummary author,
        List<String> genres, BigDecimal price) {

    public record AuthorSummary(Long id, String name) {
    }

    public static BookResponse from(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                new AuthorSummary(book.getAuthor().getId(), book.getAuthor().getName()),
                book.getGenres().stream().map(Genre::getName).sorted().toList(),
                book.getPrice());
    }
}
```

| Key                   | Why we use it                              |
| --------------------- | ------------------------------------------ |
| AuthorSummary         | Returns just an author's id and name       |
| `List<String> genres` | Returns readable genre names, not entities |
| from(Book)            | Maps the entity into an API-safe response  |

## 4. Resolve the submitted ids in `BookService`

Add `AuthorRepository` and `GenreRepository` as constructor-injected fields in `BookService`. Also add a `BookshopProperties` field: tutorial 4 used the typed settings for the shop endpoint, and the catalog settings are added here for paging.

```java
private final BookRepository bookRepository; // already present from tutorial 5
private final AuthorRepository authorRepository;
private final GenreRepository genreRepository;
private final BookshopProperties properties;

public BookService(BookRepository bookRepository, AuthorRepository authorRepository,
        GenreRepository genreRepository, BookshopProperties properties) {
    this.bookRepository = bookRepository;
    this.authorRepository = authorRepository;
    this.genreRepository = genreRepository;
    this.properties = properties;
}
```

Then update its create/update methods and add these helpers. The service looks up ids and rejects unknown ones rather than trusting client-supplied entities.

```java
public Book createBook(BookRequest request) {
    if (bookRepository.existsByTitleIgnoreCaseAndAuthorId(request.title(), request.authorId())) {
        throw ApiException.conflict("This author already has a book titled '" + request.title() + "'");
    }

    Book book = new Book(request.title(), resolveAuthor(request.authorId()), request.price());
    book.setGenres(resolveGenres(request.genreIds()));
    return bookRepository.save(book);
}

public Book updateBook(Long id, BookRequest changes) {
    Book book = getBookById(id);
    if (bookRepository.existsByTitleIgnoreCaseAndAuthorIdAndIdNot(
            changes.title(), changes.authorId(), id)) {
        throw ApiException.conflict("This author already has a book titled '" + changes.title() + "'");
    }
    book.setTitle(changes.title());
    book.setAuthor(resolveAuthor(changes.authorId()));
    book.setGenres(resolveGenres(changes.genreIds()));
    book.setPrice(changes.price());
    return bookRepository.save(book);
}

private Author resolveAuthor(Long authorId) {
    return authorRepository.findById(authorId)
            .orElseThrow(() -> ApiException.badRequest(
                    "Author with id " + authorId + " does not exist"));
}

private Set<Genre> resolveGenres(Set<Long> genreIds) {
    if (genreIds == null || genreIds.isEmpty()) {
        return new HashSet<>();
    }

    List<Genre> genres = genreRepository.findAllById(genreIds);
    if (genres.size() != genreIds.size()) {
        throw ApiException.badRequest("One or more genre ids do not exist");
    }
    return new HashSet<>(genres);
}
```

Add the corresponding imports: `HashSet`, `List`, `Set`, `Author`, `Genre`, `AuthorRepository`, and `GenreRepository`. Keep the existing `BookRepository` and `ApiException` imports.

Also import `com.example.bookshop.config.BookshopProperties`. Its tutorial-4 example only had `shopName` and `currency`, so add the catalog settings used below.

## 5. Update the repository for derived and custom queries

Add these imports and methods to `BookRepository`, which already extends `JpaRepository<Book, Long>`:

```java
import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
```

```java
List<Book> findByAuthorId(Long authorId);

boolean existsByTitleIgnoreCaseAndAuthorId(String title, Long authorId);

boolean existsByTitleIgnoreCaseAndAuthorIdAndIdNot(String title, Long authorId, Long id);

@EntityGraph(attributePaths = "author")
@Query("""
        select b from Book b
        where (:search is null or lower(b.title) like lower(concat('%', cast(:search as string), '%')))
          and (:authorId is null or b.author.id = :authorId)
          and (:genreId is null or exists (
              select 1 from Book b2 join b2.genres g
              where b2.id = b.id and g.id = :genreId))
          and (:minPrice is null or b.price >= :minPrice)
          and (:maxPrice is null or b.price <= :maxPrice)
        """)
Page<Book> search(
        @Param("search") String search,
        @Param("authorId") Long authorId,
        @Param("genreId") Long genreId,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        Pageable pageable);
```

    | Key / Annotation        | Why we use it                                      |
    |--------------------------|----------------------------------------------------|
    | `findByAuthorId`         | Derives a query from the entity's author id        |
    | `existsBy...`            | Checks for duplicate titles without loading a book |
    | `@Query`                 | Defines the combined optional-filter JPQL query    |
    | `@EntityGraph`           | Fetches the author needed by the response mapper   |
    | `Pageable` / `Page<Book>`| Adds paging/sorting and returns page/count data    |

`findByAuthorId` and `existsBy...` are derived queries: Spring builds them from the method names and entity fields. `@Query` is JPQL, so it uses entity names and Java fields. Each filter is optional when its parameter is `null`. `Pageable` supplies page, size, and sort; the returned `Page` includes the total count. `@EntityGraph` fetches the to-one author in the same query for DTO mapping.

## 6. Add paging and an allow-listed sort in the service

Add these settings to `application.properties`:

```properties
bookshop.catalog.default-page-size=10
bookshop.catalog.max-page-size=100
```

Add this nested settings class and accessor to `BookshopProperties.java`. Keep its existing shop settings and accessors:

```java
private final Catalog catalog = new Catalog();

public Catalog getCatalog() {
    return catalog;
}

public static class Catalog {
    private int defaultPageSize = 10;
    private int maxPageSize = 100;

    public int getDefaultPageSize() {
        return defaultPageSize;
    }

    public void setDefaultPageSize(int defaultPageSize) {
        this.defaultPageSize = defaultPageSize;
    }

    public int getMaxPageSize() {
        return maxPageSize;
    }

    public void setMaxPageSize(int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }
}
```

| Key                      | Why we use it                                     |
| ------------------------ | ------------------------------------------------- |
| `Catalog`                | Groups page-size configuration under `bookshop.*` |
| `getCatalog()`           | Lets `BookService` read the nested settings       |
| JavaBean getters/setters | Let Spring bind property values into the settings |

Now add this search method to `BookService`:

```java
private static final Set<String> SORTABLE_FIELDS = Set.of("id", "title", "price");

public Page<Book> getBooks(String search, Long authorId, Long genreId,
        BigDecimal minPrice, BigDecimal maxPrice, String sort, int page, Integer limit) {
    int pageIndex = Math.max(page, 1) - 1;
    int defaultSize = properties.getCatalog().getDefaultPageSize();
    int maxSize = properties.getCatalog().getMaxPageSize();
    int size = limit == null ? defaultSize : Math.min(Math.max(limit, 1), maxSize);
    Pageable pageable = PageRequest.of(pageIndex, size, parseSort(sort));
    String normalizedSearch = search == null || search.isBlank() ? null : search.trim();

    return bookRepository.search(normalizedSearch, authorId, genreId,
            minPrice, maxPrice, pageable);
}

private Sort parseSort(String sort) {
    if (sort == null || sort.isBlank()) {
        return Sort.by(Sort.Direction.DESC, "id");
    }

    List<Sort.Order> orders = new ArrayList<>();
    for (String part : sort.split(",")) {
        String value = part.trim();
        boolean descending = value.startsWith("-");
        String field = descending ? value.substring(1) : value;
        if (SORTABLE_FIELDS.contains(field)) {
            orders.add(descending ? Sort.Order.desc(field) : Sort.Order.asc(field));
        }
    }
    return orders.isEmpty() ? Sort.by(Sort.Direction.DESC, "id") : Sort.by(orders);
}
```

| Key                 | Why we use it                                       |
| ------------------- | --------------------------------------------------- |
| PageRequest         | Converts page, size, and sort into a pageable query |
| SORTABLE_FIELDS     | Allows only approved sort fields                    |
| Math.max / Math.min | Clamps page and limit to valid configured values    |
| leading `-` in sort | Requests descending order for an allowed field      |

Add imports for `ArrayList`, `BigDecimal`, `List`, `Set`, `Page`, `PageRequest`, `Pageable`, and `Sort`. A whitelist is safer than passing arbitrary client text into a sort expression.

Create `GenreRepository.java`; `findAllById` is inherited from `JpaRepository`:

```java
package com.example.bookshop.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bookshop.model.Genre;

public interface GenreRepository extends JpaRepository<Genre, Long> {
}
```

| Key                          | Why we use it                                    |
| ---------------------------- | ------------------------------------------------ |
| `JpaRepository<Genre, Long>` | Supplies `findAllById` for resolving request ids |

## 7. See and fix the N+1 query

Create `AuthorResponse.java`; mapping each author's books touches the lazy `books` collection:

```java
package com.example.bookshop.dto;

import java.util.List;

import com.example.bookshop.model.Author;
import com.example.bookshop.model.Book;

public record AuthorResponse(Long id, String name, List<String> books) {

    public static AuthorResponse from(Author author) {
        return new AuthorResponse(author.getId(), author.getName(),
                author.getBooks().stream().map(Book::getTitle).sorted().toList());
    }
}
```

| Key          | Why we use it                              |
| ------------ | ------------------------------------------ |
| id / name    | Identifies and labels the author           |
| books        | Returns the author's book titles as a list |
| from(Author) | Maps the entity into the response DTO      |

Create `AuthorRepository.java`. To reproduce N+1 first, make the service call the inherited `findAll()` and map with `AuthorResponse::from`; each lazy `author.getBooks()` access triggers another query. Check the SQL log, then use this fetch-join repository method to load authors and books together:

```java
package com.example.bookshop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.bookshop.model.Author;

public interface AuthorRepository extends JpaRepository<Author, Long> {

    @Query("select distinct a from Author a left join fetch a.books")
    List<Author> findAllWithBooks();
}
```

| Key / JPQL           | Why we use it                                    |
| -------------------- | ------------------------------------------------ |
| `left join fetch`    | Loads each author's books in the same query      |
| `distinct`           | Removes repeated author rows from the join       |
| `findAllWithBooks()` | Gives the service the fetch-planned author query |

Create `AuthorService.java`:

```java
package com.example.bookshop.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.bookshop.model.Author;
import com.example.bookshop.repository.AuthorRepository;

@Service
public class AuthorService {

    private final AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public List<Author> getAllAuthors() {
        return authorRepository.findAllWithBooks();
    }
}
```

| Key                   | Why we use it                                       |
| --------------------- | --------------------------------------------------- |
| `@Service`            | Registers business logic for dependency injection   |
| constructor injection | Supplies the repository without manual construction |
| `findAllWithBooks()`  | Avoids per-author lazy queries during mapping       |

Create `AuthorController.java`:

```java
package com.example.bookshop.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bookshop.dto.ApiResponse;
import com.example.bookshop.dto.AuthorResponse;
import com.example.bookshop.service.AuthorService;

/**
 * Lists authors and their book titles.
 *
 * | Key             | Why we use it                                      |
 * |-----------------|----------------------------------------------------|
 * | @RestController | Returns the response as JSON                       |
 * | @RequestMapping | Sets the shared `/api/v1/authors` URL prefix       |
 * | @GetMapping     | Maps GET requests to the author-list method       |
 * | AuthorResponse  | Keeps JPA entity objects out of the API            |
 */
@RestController
@RequestMapping("/api/v1/authors")
public class AuthorController {

    private final AuthorService authorService;

    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @GetMapping
    public ApiResponse<List<AuthorResponse>> getAllAuthors() {
        List<AuthorResponse> authors = authorService.getAllAuthors().stream()
                .map(AuthorResponse::from)
                .toList();

        return ApiResponse.ok("Authors fetched", authors);
    }
}
```

`distinct` removes duplicate author rows produced by the join. The controller's DTO mapping can now read the books without triggering another query per author.

Create `PageMeta.java`; it adds pagination details to the `meta` slot in `ApiResponse`:

```java
package com.example.bookshop.dto;

import org.springframework.data.domain.Page;

public record PageMeta(long total, int page, int limit, int totalPages,
        boolean hasNextPage, boolean hasPrevPage) {

    public static PageMeta from(Page<?> result) {
        return new PageMeta(
                result.getTotalElements(),
                result.getNumber() + 1,
                result.getSize(),
                Math.max(result.getTotalPages(), 1),
                result.hasNext(),
                result.hasPrevious());
    }
}
```

| Key                   | Why we use it                                           |
| --------------------- | ------------------------------------------------------- |
| `Page<?>`             | Provides total counts and current page information      |
| `getNumber() + 1`     | Converts Spring's zero-based page to one-based API page |
| `hasNext/hasPrevious` | Lets clients enable paging controls                     |

## 8. Update the book-list controller method

Replace the `GET /api/v1/books` method in `BookController`. The query parameters match the service signature above; `PageMeta.from` supplies pagination information in the response envelope.

```java
@GetMapping
public ApiResponse<List<BookResponse>> getAllBooks(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Long authorId,
        @RequestParam(required = false) Long genreId,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) String sort,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(required = false) Integer limit) {
    Page<Book> result = bookService.getBooks(
            search, authorId, genreId, minPrice, maxPrice, sort, page, limit);
    List<BookResponse> books = result.getContent().stream()
            .map(BookResponse::from)
            .toList();

    return ApiResponse.ok("Books fetched", books, PageMeta.from(result));
}
```

| Key / Annotation | Why we use it                                  |
| ---------------- | ---------------------------------------------- |
| `@RequestParam`  | Reads optional filters and paging from the URL |
| `Page<Book>`     | Provides current results and paging totals     |
| `PageMeta.from`  | Adds paging details to the response envelope   |

Required imports for this method: `BigDecimal`, `List`, `Page`, `GetMapping`, `RequestParam`, `ApiResponse`, `BookResponse`, `PageMeta`, and `Book` as needed by the rest of the controller. For example, `?search=clean&authorId=2&sort=-price&page=1&limit=5` searches and sorts the first page.

## 9. Update the seed data

The old tutorial-5 `data.sql` inserts author names directly into `book.author`. Replace it with inserts for authors and genres first, then reference them by id. This example uses subqueries so it does not depend on generated ids:

```sql
INSERT INTO author (name) VALUES
  ('Joshua Bloch'),
  ('Robert C. Martin'),
  ('Andrew Hunt'),
  ('Martin Fowler');

INSERT INTO genre (name) VALUES
  ('Programming'),
  ('Java'),
  ('Software Design');

INSERT INTO book (title, author_id, price, cost_price)
SELECT 'Effective Java', id, 54.99, 31.00 FROM author WHERE name = 'Joshua Bloch';

INSERT INTO book (title, author_id, price, cost_price)
SELECT 'Clean Code', id, 42.50, 15.00 FROM author WHERE name = 'Robert C. Martin';

INSERT INTO book (title, author_id, price, cost_price)
SELECT 'The Pragmatic Programmer', id, 49.95, 28.50 FROM author WHERE name = 'Andrew Hunt';

INSERT INTO book (title, author_id, price, cost_price)
SELECT 'Clean Architecture', id, 39.99, 22.00 FROM author WHERE name = 'Robert C. Martin';

INSERT INTO book (title, author_id, price, cost_price)
SELECT 'Refactoring', id, 52.00, 30.10 FROM author WHERE name = 'Martin Fowler';

INSERT INTO book_genre (book_id, genre_id)
SELECT b.id, g.id FROM book b CROSS JOIN genre g
WHERE (b.title = 'Effective Java' AND g.name IN ('Programming', 'Java'))
   OR (b.title = 'Clean Code' AND g.name IN ('Programming', 'Software Design'))
   OR (b.title = 'The Pragmatic Programmer' AND g.name = 'Programming')
   OR (b.title = 'Clean Architecture' AND g.name = 'Software Design')
   OR (b.title = 'Refactoring' AND g.name IN ('Programming', 'Software Design'));
```

The script runs after Hibernate creates the tables, as configured in tutorial 5. In tutorial 18, these inserts move into Flyway migration files.

## 10. Verify the endpoints

```bash
curl 'http://localhost:8080/api/v1/books?search=clean&authorId=2&sort=-price&page=1&limit=5'
curl 'http://localhost:8080/api/v1/authors'
```

The book list includes flattened author/genre data and pagination metadata. The authors endpoint returns book titles without an N+1 query after the fetch-join change.

## Common mistakes

- Leaving `Book.author` as a `String` after changing the request to `authorId`.
- Omitting `mappedBy = "author"` from `Author.books`, which creates an unnecessary extra join table.
- Returning entities directly and triggering recursive serialization or lazy-loading queries.
- Forgetting to replace tutorial 5 seed SQL after `book.author` becomes a foreign key.
- Using incomplete derived-query names or misspelling an entity field; Spring Data detects invalid names at startup.

Next: [**Tutorial 11 — Soft delete**](tutorial-11%20%28Soft%20delete%29.md)

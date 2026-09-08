# Tutorial 10 — Relationships and queries

OneToMany / ManyToMany, the N+1 problem caught in the SQL log,
derived queries, @Query, and pagination + whitelisted
search/filter/sort with `meta`.

Files for this stage:
- `model/Author.java`, `model/Genre.java` (new), `model/Book.java`
  (author is now a real reference)
- `repository/AuthorRepository.java`, `GenreRepository.java` (new),
  `BookRepository.java` (three query styles)
- `service/AuthorService.java`, `controller/AuthorController.java`
  (new), `service/BookService.java` (query features + rules),
  `controller/BookController.java` (list endpoint learns knobs)
- `dto/*` (BookRequest/BookResponse reshaped; PageMeta,
  AuthorResponse new), `data.sql` (four tables)

---

## 1. The data model grows up

"author" was a text column. Now it is a table, because authors have
their own life (their own endpoint, maybe a bio later). And books
get genres — a categorization several books share.

```
 author                book             book_genre         genre
+----+---------+      +----+-----------+ +---------+------+ +----+------+
| id | name    |      | id | title     | | book_id |genre | | id | name |
+----+---------+      |    | author_id | |         | _id  | +----+------+
      ^               |    | price     | +---------+------+
      |               |    | cost_price|      ^        ^
      +-- one-to-many-+----+-----------+      +--------+-- many-to-many
          (author_id points at author)         (join table: one row per pair)
```

Two relationship kinds, two mechanisms:

```
+---------------------+----------------------------------------------+
| one-to-many         | a foreign-key COLUMN on the "many" side:     |
| author <- books     | book.author_id                               |
+---------------------+----------------------------------------------+
| many-to-many        | a JOIN TABLE with one row per pair:          |
| books <-> genres    | book_genre(book_id, genre_id)                |
+---------------------+----------------------------------------------+
```

## 2. Mapping them in JPA

**The owning side.** `Book.author` carries the annotation that maps
the real column, so Book OWNS the relationship:

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "author_id")
private Author author;
```

`Author.books` is only a mirror — `mappedBy` says "the mapping lives
on Book.author, don't invent anything":

```java
@OneToMany(mappedBy = "author")
private List<Book> books = new ArrayList<>();
```

! Forget `mappedBy` and JPA silently creates a THIRD table
(`author_books`) tracking the same fact twice. Mystery join table in
your schema = missing mappedBy.

**Many-to-many.** Book owns this one too and declares the join
table; `Set` not `List` (a genre applies once, no order):

```java
@ManyToMany(fetch = FetchType.LAZY)
@BatchSize(size = 50)
@JoinTable(name = "book_genre",
        joinColumns = @JoinColumn(name = "book_id"),
        inverseJoinColumns = @JoinColumn(name = "genre_id"))
private Set<Genre> genres = new HashSet<>();
```

**LAZY vs EAGER, the rule that prevents pain:**

```
+---------------------------------+----------------------------------+
| EAGER                           | LAZY                             |
+---------------------------------+----------------------------------+
| related data loads WITH the     | a placeholder (proxy) sits there |
| owner, every time, needed or not| until first touched, THEN a      |
|                                 | query runs                       |
| JPA default for @ManyToOne (!)  | JPA default for collections      |
+---------------------------------+----------------------------------+
```

+ Make EVERYTHING lazy (note the explicit `fetch = LAZY` on
`@ManyToOne`), then fetch eagerly PER QUERY where a use case needs
it. Loading policy belongs to queries, not to the entity.

## 3. DTOs absorb the change

Clients reference related things BY ID inbound, and get them
flattened outbound:

```json
// in (BookRequest)                    // out (BookResponse.data)
{"title": "...",                       {"id": 6, "title": "...",
 "authorId": 2,                         "author": {"id":2,"name":"Robert C. Martin"},
 "genreIds": [1, 2],                    "genres": ["Java","Programming"],
 "price": 39.99}                        "price": 39.99}
```

**Why not just serialize the entities?** I tried it, for science —
made the authors endpoint return raw `Author` entities. Real result:

```
HTTP 200, bytes received: 11730
log: ...nesting depth (501) exceeds the maximum allowed (500)...
```

Jackson walked Author → books → each book's author → its books → ...
in circles until it hit a depth limit of 500 — after already
streaming 11 KB of a "successful" response. A 200 status with a
truncated garbage body is the worst failure mode in this course so
far. DTOs make it structurally impossible (`from(...)` copies fields,
it does not walk graphs).

## 4. N+1 — caught red-handed

`GET /api/v1/authors` returns each author with their book titles.
First implementation: `authorRepository.findAll()`, then the mapper
touches `author.getBooks()` (lazy). The SQL log, real, for FOUR
authors:

```
Hibernate: select a1_0.id,a1_0.name from author a1_0
Hibernate: select ... from book b1_0 where b1_0.author_id=?
Hibernate: select ... from book b1_0 where b1_0.author_id=?
Hibernate: select ... from book b1_0 where b1_0.author_id=?
Hibernate: select ... from book b1_0 where b1_0.author_id=?
-- query count: 5
```

1 query for the list + N queries for the lazy collections = **N+1**.
The code looks innocent, the page works, and with 4,000 authors it
runs 4,001 queries. This is the most common performance bug in JPA
apps, and the SQL log is how you catch it.

**The fix — fetch join.** One method in `AuthorRepository`:

```java
@Query("select distinct a from Author a left join fetch a.books")
List<Author> findAllWithBooks();
```

Same endpoint after the fix:

```
Hibernate: select distinct a1_0.id,b1_0.author_id,b1_0.id,...,a1_0.name from ...
-- query count: 1
```

Five queries became one. (`distinct` because the join repeats each
author once per book.)

**The list endpoint's version.** `/api/v1/books` needs each book's
author (to-one) and genres (to-many) — but collection fetch joins
don't mix with pagination. Two tools instead:

- `@EntityGraph(attributePaths = "author")` on the search query:
  join the author in the same SQL — safe, it's to-one.
- `@BatchSize(size = 50)` on `genres`: when one book's genres load,
  Hibernate loads genres for up to 50 books in a single IN-query.

Verified — one full list request, six books with authors and genres:

```
-- SQL queries for one list request: 2
Hibernate: select b1_0.id,b1_0.author_id,a1_0.id,a1_0.name,... from book b1_0 join author ...
Hibernate: select g1_0.book_id,g1_1.id,g1_1.name from book_genre g1_0 join genre g1_1 ...
```

## 5. Three ways to define a query

**1. Derived from the method name** — Spring Data parses the name
and writes the query:

```java
List<Book> findByAuthorId(Long authorId);
boolean existsByTitleIgnoreCaseAndAuthorId(String title, Long authorId);
boolean existsByTitleIgnoreCaseAndAuthorIdAndIdNot(String title, Long authorId, Long id);
```

Vocabulary: `By`, `And`, `Or`, `Containing`, `IgnoreCase`,
`LessThan`, `Between`, `OrderBy`... Misspell a field and the app
refuses to start — verified: `findByAutor` produced
`No property 'autor' found for type 'Book'` at boot. Annoying but
honest; you cannot ship a typo.

**2. @Query (JPQL)** — SQL written over entities and their FIELDS
(`Book`, `b.title`), not tables. Ours powers the list endpoint, with
every filter optional:

```java
@EntityGraph(attributePaths = "author")
@Query("""
        select b from Book b
        where (:search is null or lower(b.title) like lower(concat('%', :search, '%')))
          and (:authorId is null or b.author.id = :authorId)
          and (:genreId is null or :genreId in (select g.id from b.genres g))
          and (:minPrice is null or b.price >= :minPrice)
          and (:maxPrice is null or b.price <= :maxPrice)
        """)
Page<Book> search(..., Pageable pageable);
```

The `(:param is null or condition)` pattern = "filter only when the
client sent it". One query serves every filter combination — and
NOTHING outside these five hand-written conditions is possible, so
there is nothing to inject.

**3. Pageable** — pass "page 1, 10 rows, price desc" as a parameter,
get a `Page<Book>` back: the rows plus the total count (Spring runs
the count query when it needs to).

## 6. The query features (queryFeatures.js, ported)

`GET /api/v1/books` now takes:

```
+-----------+----------------------------+-------------------------------+
| Param     | Example                    | Behavior                      |
+-----------+----------------------------+-------------------------------+
| search    | ?search=clean              | title contains, case-insens.  |
| authorId  | ?authorId=2                | exact match                   |
| genreId   | ?genreId=2                 | book has that genre           |
| minPrice  | ?minPrice=45               | price >= (maxPrice likewise)  |
| sort      | ?sort=-price,title         | whitelist: id, title, price;  |
|           |                            | "-" prefix = descending;      |
|           |                            | unknown fields dropped        |
| page      | ?page=2                    | 1-based                       |
| limit     | ?limit=5                   | default 10, HARD CAP 100      |
|           |                            | (both from BookshopProperties)|
+-----------+----------------------------+-------------------------------+
```

All verified live; highlights:

```
?search=clean          -> Clean Architecture, Clean Code
?genreId=2             -> Effective Java
?sort=-price,title     -> 54.99, 52.0, 49.95, 42.5, 39.99
?sort=costPrice        -> IGNORED (not whitelisted) - falls back to newest-first;
                          internal fields cannot even be probed via sorting
?page=2&limit=2        -> meta: {"total":5,"page":2,"limit":2,"totalPages":3,
                                 "hasNextPage":true,"hasPrevPage":true}
?limit=1000            -> meta.limit = 100 (the tutorial-04 config cap, enforced)
```

`meta` fills the envelope slot reserved since tutorial 07 — same
fields as the Node reference, straight from `PageMeta.from(page)`.

## 7. Real business rules, real status codes

The service finally has rules beyond "shape is valid" (these NEED
the database, which is why they live here and not in annotations):

```
POST duplicate ("clean code" by author 2 again, case-insensitive):
{"success":false,"message":"This author already has a book titled 'clean code'"} | 409

POST {"authorId": 99}:
{"success":false,"message":"Author with id 99 does not exist"} | 400

POST {"genreIds": [42]}:
{"success":false,"message":"One or more genre ids do not exist"} | 400
```

## 8. The common mistakes

1. **N+1** — the headline mistake of this stage; see section 4. You
   find it by READING THE SQL LOG, never by reading the code.
2. **Serializing entities with relationships** — nesting-depth
   explosion after a 200 status (section 3). DTOs prevent it.
3. **Missing `mappedBy`** — ghost join table appears in the schema.
4. **Derived-name typo** — app refuses to boot; read the "No
   property" message, it names the exact bad piece.

## 9. Recap

- FK column = one-to-many; join table = many-to-many; the side with
  the column/table owns the mapping, the mirror gets `mappedBy`.
- Everything LAZY on entities; each query fetches what IT needs
  (fetch join / @EntityGraph; @BatchSize for paginated collections).
- Watch the SQL log. N+1 hides in innocent-looking mappers.
- Derived names for simple lookups, @Query + optional-filter pattern
  for the list engine, Pageable for paging; whitelists everywhere.
- Clients send ids in, get flattened summaries out; meta completes
  the envelope.

Next: [**Tutorial 11 — Soft delete**](tutorial-11%20%28Soft%20delete%29.md): stop losing data on DELETE,
with Hibernate's built-in @SoftDelete — and a restore endpoint.

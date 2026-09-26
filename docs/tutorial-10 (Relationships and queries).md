# Tutorial 10 — Relationships and queries

Add real relationships and filterable queries.

Files for this stage:

- New: `src/main/java/com/example/bookshop/model/Author.java`
- New: `src/main/java/com/example/bookshop/model/Genre.java`
- Updated: `src/main/java/com/example/bookshop/model/Book.java`
- New: `src/main/java/com/example/bookshop/repository/AuthorRepository.java`
- Updated: `src/main/java/com/example/bookshop/repository/BookRepository.java`
- Updated: `src/main/java/com/example/bookshop/controller/BookController.java`

---

## 1. Book owns the relationship

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "author_id")
private Author author;
```

```java
@ManyToMany(fetch = FetchType.LAZY)
@JoinTable(name = "book_genre",
        joinColumns = @JoinColumn(name = "book_id"),
        inverseJoinColumns = @JoinColumn(name = "genre_id"))
private Set<Genre> genres = new HashSet<>();
```

This is the owning side of the relationship.

## 2. Add a search query

```java
@EntityGraph(attributePaths = "author")
@Query("""
        select b from Book b
        where (:search is null or lower(b.title) like lower(concat('%', cast(:search as string), '%')))
          and (:authorId is null or b.author.id = :authorId)
          and (:genreId is null or exists (
              select 1 from Book b2 join b2.genres g
              where b2.id = b.id and g.id = :genreId))
        """)
Page<Book> search(..., Pageable pageable);
```

This lets the list endpoint filter by title, author, genre, and price.

## 3. Query parameters on the controller

```java
@GetMapping
public ApiResponse<List<BookResponse>> getAllBooks(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Long authorId,
        @RequestParam(required = false) Long genreId,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(required = false) Integer limit) {
    Page<Book> result = bookService.getBooks(search, authorId, genreId, minPrice, maxPrice, page, limit);
    return ApiResponse.ok("Books fetched", ..., PageMeta.from(result));
}
```

This is the API search/filter style for the book list.

Next: [**Tutorial 11 — Soft delete**](tutorial-11%20%28Soft%20delete%29.md)

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

# Tutorial 11 — Soft delete

Hide rows instead of deleting them permanently using Hibernate's `@SoftDelete`. Learn how to implement restore endpoints, query soft-deleted records using native SQL escape hatches, and avoid data loss in production.

Files for this stage:

- Updated: `src/main/java/com/example/bookshop/model/Book.java`
- Updated: `src/main/java/com/example/bookshop/repository/BookRepository.java`
- Updated: `src/main/java/com/example/bookshop/service/BookService.java`
- Updated: `src/main/java/com/example/bookshop/controller/BookController.java`

---

## 1. Why Real Products Rarely DELETE

A hard SQL `DELETE` is permanent: audit history is destroyed, foreign keys in orders or references from other tables break, and there is no way to undo the deletion when a customer or staff member makes a mistake.

> **Soft delete** = adding a status column (e.g., `deleted boolean not null default false`) that queries automatically filter on. "Deleted" rows remain in the database table for recovery and audit trails, but are completely invisible to the application's normal queries.

In Hibernate 6.4+, soft delete is built directly into the framework with a single annotation:

```java
package com.example.bookshop.model;

import org.hibernate.annotations.SoftDelete;
import jakarta.persistence.Entity;

/**
 * | Key                         | Why we use it                                    |
 * |-----------------------------|--------------------------------------------------|
 * | @Entity                     | Maps Book objects to database rows               |
 * | @SoftDelete(columnName=...) | Marks rows instead of physically removing them   |
 */
@Entity
@SoftDelete(columnName = "deleted")
public class Book {
    // ...
}
```

Hibernate now treats deletes as updates instead of hard deletes.

### Three Automatic Behaviors Enabled by `@SoftDelete`
1. **Schema Alteration**: Hibernate expects a `deleted boolean not null` column on the `book` table (and its link tables).
2. **Deletes Become Updates**: Calling `bookRepository.delete(book)` executes an SQL `UPDATE book SET deleted = true WHERE id = ? AND deleted = false`.
3. **Transparent Query Filtering**: Every standard Hibernate query automatically appends `AND deleted = false` behind the scenes — including `findAll()`, `findById()`, derived queries like `findByAuthorId()`, our custom `search()` JPQL query, and pagination `COUNT` queries.

---

## 2. Add Native Query Escape Hatches to `BookRepository`

Because Hibernate transparently filters out soft-deleted records, standard JPQL methods cannot find or update rows where `deleted = true`. To view or restore deleted books, we use **native SQL queries** as an escape hatch, because native queries bypass Hibernate's automatic soft-delete filter:

Add these methods to `src/main/java/com/example/bookshop/repository/BookRepository.java`:

```java
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

    @Query(value = "select * from book where deleted = true", nativeQuery = true)
    List<Book> findDeleted();

    @Modifying
    @Query(value = "update book set deleted = false where id = :id and deleted = true", nativeQuery = true)
    int restoreById(@Param("id") Long id);

    @Modifying
    @Query(value = "update book_genre set deleted = false where book_id = :id and deleted = true", nativeQuery = true)
    int restoreGenreLinks(@Param("id") Long id);
```

| Key / Annotation   | Why we use it                                       |
| ------------------ | --------------------------------------------------- |
| `nativeQuery=true` | Runs raw SQL that bypasses Hibernate's soft-delete filter |
| `@Modifying`       | Marks the query as a database write returning affected row count |
| `@Param("id")`     | Binds the Java `id` argument to SQL's `:id` parameter |
| `restoreGenreLinks`| Revives join table rows so restored books retain their genres |

This lets the app list deleted rows and restore them without exposing them in normal queries.

---

## 3. Implement Restore & Deleted Methods in `BookService`

Add these methods to `src/main/java/com/example/bookshop/service/BookService.java`:

```java
import org.springframework.transaction.annotation.Transactional;

    public List<Book> getDeletedBooks() {
        return bookRepository.findDeleted();
    }

    @Transactional
    public Book restoreBook(Long id) {
        int restored = bookRepository.restoreById(id);
        if (restored == 0) {
            throw ApiException.notFound("No deleted book with id " + id);
        }
        bookRepository.restoreGenreLinks(id);
        return getBookById(id);
    }
```

| Key / Method | Explanation |
| :--- | :--- |
| `getDeletedBooks()` | Fetches all soft-deleted books for management/admin views. |
| `@Transactional` | Required for `@Modifying` queries. Ensures both the book and its genre links are restored atomically. |
| `restored == 0` | If no row was updated (e.g. book does not exist or was not deleted), returns a clean 404. |

---

## 4. Add Endpoints to `BookController`

Add the deleted-list and restore endpoints to `src/main/java/com/example/bookshop/controller/BookController.java`:

```java
    @GetMapping("/deleted")
    public ApiResponse<List<BookResponse>> getDeletedBooks() {
        List<BookResponse> books = bookService.getDeletedBooks().stream()
                .map(BookResponse::from)
                .toList();
        return ApiResponse.ok("Deleted books fetched", books);
    }

    @PostMapping("/{id}/restore")
    public ApiResponse<BookResponse> restoreBook(@PathVariable Long id) {
        Book restored = bookService.restoreBook(id);
        return ApiResponse.ok("Book restored", BookResponse.from(restored));
    }
```

---

## 5. Normal Reads Stay Clean & Verified Lifecycle

### Step 1: Delete a book
```bash
curl -X DELETE http://localhost:8080/api/v1/books/5
```
Response:
```json
{
  "success": true,
  "message": "Book deleted"
}
```

The SQL that actually ran in the database:
```sql
Hibernate: update book_genre set deleted=true where book_id=? and deleted=false
Hibernate: update book set deleted=true where id=? and deleted=false
```

Note that Hibernate soft-deleted both the book and its `book_genre` join table rows!

### Step 2: Normal queries hide the deleted book
```bash
curl -i http://localhost:8080/api/v1/books/5
# Returns 404 Not Found

curl -s http://localhost:8080/api/v1/books
# The book no longer appears, and pagination 'meta.total' decrements by 1
```

After that, the book no longer appears in normal list/get requests, but it still exists in the DB for recovery.

### Step 3: View deleted books
```bash
curl -s http://localhost:8080/api/v1/books/deleted
```
Response:
```json
{
  "success": true,
  "message": "Deleted books fetched",
  "data": [
    {
      "id": 5,
      "title": "Refactoring"
    }
  ]
}
```

### Step 4: Restore the book
```bash
curl -X POST http://localhost:8080/api/v1/books/5/restore
```
Response:
```json
{
  "success": true,
  "message": "Book restored",
  "data": {
    "id": 5,
    "title": "Refactoring",
    "genres": ["Programming", "Software Design"]
  }
}
```

The book is back in the active catalog with its genres intact!

---

## 6. Two Real Gotchas to Keep in Mind

### Gotcha 1: Raw SQL seeds bypass Hibernate
When writing raw SQL (e.g. `data.sql` or Flyway migrations), SQL executes directly in the database without going through Hibernate. If a column is defined as `deleted boolean not null`, your raw SQL INSERT statements must explicitly specify `deleted` or have a database `DEFAULT false` constraint:
```sql
ALTER TABLE book ADD COLUMN deleted BOOLEAN DEFAULT FALSE NOT NULL;
```

### Gotcha 2: Restoring join relationships
Soft-deleting an entity that has `@ManyToMany` relationships (like `Book.genres`) soft-deletes the link table rows (`book_genre`). If you only restore `book` with `update book set deleted = false`, the restored book will have empty genres! This is why `restoreGenreLinks` must be executed alongside `restoreById` inside the same `@Transactional` method.

---

## Summary Table

| Key / Annotation | Why we use it |
| :--- | :--- |
| `@SoftDelete(columnName = "deleted")` | Converts `delete()` to SQL updates and auto-appends `AND deleted = false` to queries. |
| `nativeQuery = true` | Bypasses Hibernate filters to read and modify soft-deleted records. |
| `@Modifying` | Tells Spring Data that a repository query modifies data and returns an update count. |
| `@Transactional` | Binds the restore steps into an atomic unit. |

Next: [**Tutorial 12 — Transactions**](tutorial-12%20%28Transactions%29.md)

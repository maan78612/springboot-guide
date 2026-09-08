# Tutorial 11 — Soft delete

@SoftDelete, a restore endpoint, and listing deleted rows.

Files for this stage:
- `model/Book.java` (+ @SoftDelete)
- `repository/BookRepository.java` (+ two native-query escape hatches)
- `service/BookService.java` (+ restore/getDeleted)
- `controller/BookController.java` (+ GET /deleted, POST /{id}/restore)
- `data.sql` (must fill the new columns — see the gotcha)

---

## 1. Why real products rarely DELETE

A hard `DELETE` is forever: audit history gone, references from
other tables broken, no undo when a customer calls. So production
systems usually *mark* rows dead instead of removing them:

> **Soft delete** = an extra column (`deleted`) that every query
> filters on. "Deleted" rows exist in the database but are invisible
> to the application.

Your Node repo needed a 40-line hand-written Mongoose plugin for
this (add fields, hook five query types, add restore methods). In
Hibernate it is built in — one annotation:

```java
@Entity
@SoftDelete(columnName = "deleted")
public class Book { ... }
```

That single line changes three behaviors, all verified below:

1. the schema gains `deleted boolean not null`,
2. `repository.delete(book)` becomes an `UPDATE`, not a `DELETE`,
3. EVERY Hibernate query silently appends `and deleted = false` —
   `findAll`, `findById`, derived queries, our `search()` @Query,
   the pagination counts. All of them.

## 2. Verified: the lifecycle

```
DELETE /api/v1/books/5
{"success":true,"message":"Book deleted"} | 200

The SQL that actually ran (from the log):
Hibernate: update book_genre set deleted=true where book_id=? and deleted=false
Hibernate: update book set deleted=true where id=? and deleted=false

GET /api/v1/books/5          -> 404          (invisible)
GET /api/v1/books            -> meta.total: 4 (counts respect it too)
GET /api/v1/books/deleted    -> ["Refactoring"]
POST /api/v1/books/5/restore -> 200, book is back, meta.total: 5
POST /api/v1/books/1/restore -> 404 "No deleted book with id 1"
```

Note the delete SQL: not one UPDATE but two. Hibernate soft-deletes
the `book_genre` JOIN ROWS as well — the annotation covers the
collections the entity owns.

## 3. The escape hatches: seeing and reviving deleted rows

Soft-deleted rows are invisible to Hibernate — including when you
WANT to see them (an admin screen, a restore). The way out is native
SQL, which Hibernate does not rewrite:

```java
@Query(value = "select * from book where deleted = true", nativeQuery = true)
List<Book> findDeleted();

@Modifying
@Query(value = "update book set deleted = false where id = :id and deleted = true",
        nativeQuery = true)
int restoreById(@Param("id") Long id);
```

Details that carry weight:

- `@Modifying` marks a query that writes; it returns the number of
  rows changed. The service reads that: `0` rows → there was no
  deleted book with that id → 404. And because of
  `and deleted = true` in the WHERE, "restoring" a live book is a
  clean no-op-404 instead of a silent lie.
- `@Modifying` queries must run inside a transaction — the service's
  restore method wears `@Transactional`. Full story next tutorial.

## 4. Two real bugs found by running (both now teaching material)

**Bug 1 — the seed broke the boot.** After adding `@SoftDelete` the
app refused to start:

```
Failed to execute SQL script statement #3 of file ... data.sql:
NULL not allowed for column "DELETED"
```

`data.sql` is raw SQL. Hibernate rewrites ITS OWN statements to
manage `deleted`, but a seed script bypasses Hibernate completely —
so the INSERTs must supply `deleted` themselves (`..., false`). And
then the boot failed AGAIN on statement #4: the join table
`book_genre` has its own `deleted` column (see the delete SQL above)
and needed the same fix. Any raw-SQL tool — seeds, migrations,
psql — must know about soft-delete columns.

**Bug 2 — restore lost the genres.** First successful restore
returned:

```
{"id":5,"title":"Refactoring","genres":[], ...}
```

Empty genres! Deleting had soft-deleted the join rows, and my
restore revived only the `book` row. Fix: a second native update
(`restoreGenreLinks`) inside the SAME `@Transactional` method —
restore is two writes that must succeed or fail together. Verified
after the fix:

```
Refactoring ['Programming', 'Software Design']
```

## 5. Design consequences to keep in mind

- The duplicate-title check (tutorial 10) runs through Hibernate, so
  it ignores deleted books. You can create "Refactoring" by the same
  author while the original lies deleted — and then restoring the
  original produces a real duplicate. Options in a real product:
  block the restore with a 409, or include deleted rows in the
  uniqueness check. We keep it simple and accept it — but it is a
  DECISION, and soft delete forces several like it.
- Deleted rows still occupy the table and its indexes. Big systems
  eventually archive or purge them; "soft" is not "free".
- `GET /books/deleted` and restore are for staff, not the public —
  they become admin-only in tutorial 16.

## 6. Recap

- `@SoftDelete(columnName = "deleted")`: schema, deletes and ALL
  queries handled — the Mongoose plugin from the Node repo, as one
  built-in annotation.
- Join rows of owned collections are soft-deleted too.
- Native queries are the deliberate escape hatch for admin/restore —
  and a trap if you forget they see everything.
- Seeds and any raw SQL must fill `deleted` columns by hand.
- Restore = every write it takes to undo the delete, in ONE
  transaction.

Next: [**Tutorial 12 — Transactions**](tutorial-12%20%28Transactions%29.md): what @Transactional really
does, when a rollback happens (verified with a deliberate crash),
and the lazy-loading trap.

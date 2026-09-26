# Tutorial 11 — Soft delete

Hide rows instead of deleting them permanently.

Files for this stage:

- Updated: `src/main/java/com/example/bookshop/model/Book.java`
- Updated: `src/main/java/com/example/bookshop/repository/BookRepository.java`
- Updated: `src/main/java/com/example/bookshop/service/BookService.java`
- Updated: `src/main/java/com/example/bookshop/controller/BookController.java`

---

## 1. Mark the entity for soft delete

```java
@Entity
@SoftDelete(columnName = "deleted")
public class Book {
    // ...
}
```

Hibernate now treats deletes as updates instead of hard deletes.

## 2. Add restore endpoints

```java
@Query(value = "select * from book where deleted = true", nativeQuery = true)
List<Book> findDeleted();

@Modifying
@Query(value = "update book set deleted = false where id = :id and deleted = true", nativeQuery = true)
int restoreById(@Param("id") Long id);
```

This lets the app list deleted rows and restore them without exposing them in normal queries.

## 3. Normal reads stay clean

```java
DELETE /api/v1/books/5
{"success":true,"message":"Book deleted"}
```

After that, the book no longer appears in normal list/get requests, but it still exists in the DB for recovery.

Next: [**Tutorial 12 — Transactions**](tutorial-12%20%28Transactions%29.md)

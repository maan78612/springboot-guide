# Tutorial 12 — Transactions

A transaction must either commit fully or roll back completely.

Files for this stage:

- Updated: `src/main/java/com/example/bookshop/service/BookService.java`
- New: `src/main/java/com/example/bookshop/controller/AuthorController.java`
- New: `src/main/java/com/example/bookshop/dto/DiscountRequest.java`

---

## 1. Put the whole write inside one transaction

```java
@Transactional
public List<Book> applyAuthorDiscount(Long authorId, int percent) {
    // loop through books
    // validate price floor
    // update each book
}
```

If one book violates the rule, nothing should be committed.

## 2. Why this matters

Without `@Transactional`, each change can commit separately and leave partial data behind.

```text
request says "discount failed"
actual DB: one book already updated
```

The transaction boundary solves that.

Next: [**Tutorial 13 — Testing**](tutorial-13%20%28Testing%29.md)

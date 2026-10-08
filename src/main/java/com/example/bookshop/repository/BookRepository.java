package com.example.bookshop.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.bookshop.model.Book;

/**
 * Repository for Book entities with derived queries, JPQL search, and pagination.
 *
 * | Key / Annotation        | Why we use it                                      |
 * |-------------------------|----------------------------------------------------|
 * | JpaRepository<Book, Long>| Supplies standard CRUD methods for Book and its ID|
 * | findByAuthorId          | Derives a query from the entity's author id        |
 * | existsBy...             | Checks for duplicate titles without loading a book |
 * | @Query                  | Defines the combined optional-filter JPQL query    |
 * | @EntityGraph            | Fetches the author needed by the response mapper   |
 * | Pageable / Page<Book>   | Adds paging/sorting and returns page/count data    |
 */
public interface BookRepository extends JpaRepository<Book, Long> {

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

  @Query(value = "select * from book where deleted = true", nativeQuery = true)
  List<Book> findDeleted();

  @Modifying
  @Query(value = "update book set deleted = false where id = :id and deleted = true", nativeQuery = true)
  int restoreById(@Param("id") Long id);
}

/*
 ^ TUTORIAL 05 — the repository is an interface; Spring Data
 ? generates the implementation from the type arguments at startup.
 ? Free: findAll, findById, save, delete, count, existsById...

 ^ TUTORIAL 10 — three ways to define a query

 ? 1. DERIVED: write a method name in Spring Data's vocabulary and
 ?    the query is generated FROM THE NAME:
 ?      findByAuthorId(5)                 -> where author_id = 5
 ?      existsByTitleIgnoreCaseAndAuthorId -> select exists(...)
 ?    Vocabulary: By, And, Or, Containing, IgnoreCase, LessThan,
 ?    Between, OrderBy... Great until the name stops fitting on a line.
 ! A typo in the FIELD name (findByAutor...) fails AT STARTUP:
 !    "No property 'autor' found for type 'Book'" - annoying but
 !    honest. You cannot ship a misspelled derived query.

 ? 2. @Query: you write JPQL (SQL over entities and their FIELDS,
 ?    not tables/columns). Used for search(): every filter is
 ?    optional via the "(:param is null or ...)" pattern - one query
 ?    serves search + author + genre + price range in any combination.
 ?    Only WHITELISTED things are possible because each filter is
 ?    written out by hand - a client cannot inject anything.

 ? 3. Pageable: pass "page 2, size 10, sorted by price desc" as a
 ?    parameter and get a Page<Book> back: the rows PLUS total count
 ?    (Spring runs a second count query automatically).

 ? @EntityGraph(attributePaths = "author") on search():
 ?    "when running this query, fetch the author in the same SQL
 ?    join" - kills the would-be N+1 on the list endpoint. Safe with
 ?    pagination because author is to-ONE (no row multiplication).
*/
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

public interface BookRepository extends JpaRepository<Book, Long> {

	List<Book> findByAuthorId(Long authorId);

	boolean existsByTitleIgnoreCaseAndAuthorId(String title, Long authorId);

	// same check for updates: "another book (IdNot = not me) already
	// has this title by this author"
	boolean existsByTitleIgnoreCaseAndAuthorIdAndIdNot(String title, Long authorId, Long id);

	/*
	 ^ TUTORIAL 11 — the two deliberate escape hatches from @SoftDelete
	 ? nativeQuery = true means raw SQL against the real table -
	 ? Hibernate does not add "deleted = false" here. Only these two
	 ? methods can see or touch deleted rows.
	 ? @Modifying marks a query that WRITES; it returns the number of
	 ? rows changed. "and deleted = true" makes restoring a live book
	 ? a no-op (0 rows) -> the service turns that into a 404.
	 */
	@Query(value = "select * from book where deleted = true", nativeQuery = true)
	List<Book> findDeleted();

	@Modifying
	@Query(value = "update book set deleted = false where id = :id and deleted = true",
			nativeQuery = true)
	int restoreById(@Param("id") Long id);

	/*
	 ! Found by testing: deleting a book also soft-deletes its
	 ! book_genre join rows, so a restored book came back with NO
	 ! genres. Restore must revive the join rows too - both updates
	 ! run inside one @Transactional service method (tutorial 12).
	 */
	@Modifying
	@Query(value = "update book_genre set deleted = false where book_id = :id",
			nativeQuery = true)
	void restoreGenreLinks(@Param("id") Long id);

	/*
	 ! cast(:search as string) is not decoration. PostgreSQL must know
	 ! every parameter's type; a null :search arrives untyped, Postgres
	 ! guesses "bytea", and lower(bytea) explodes:
	 !   ERROR: function lower(bytea) does not exist
	 ! H2 never complained - found the day this query first met real
	 ! Postgres (tutorial 18). The cast pins the type either way.
	 */
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
}

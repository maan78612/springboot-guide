/*
 ^ TUTORIAL 10 — the fetch-join fix for N+1

 ? findAll() + author.getBooks() per author = 1 query for authors,
 ? then 1 MORE query per author for their books: "N+1". Invisible in
 ? code, obvious in the SQL log, deadly with real row counts.

 + findAllWithBooks(): "left join fetch" loads authors AND their
 +   books in ONE SQL join. distinct because the join would otherwise
 +   repeat each author once per book.
 * Rule: collections stay LAZY on the entity; each QUERY that needs
 *   them fetches them explicitly.
*/
package com.example.bookshop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.bookshop.model.Author;

public interface AuthorRepository extends JpaRepository<Author, Long> {

	@Query("select distinct a from Author a left join fetch a.books")
	List<Author> findAllWithBooks();
}

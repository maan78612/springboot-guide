package com.example.bookshop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.bookshop.model.Author;

/**
 * Repository for Author entities with fetch-join query support.
 *
 * | Key / JPQL         | Why we use it                                    |
 * |--------------------|--------------------------------------------------|
 * | JpaRepository      | Provides basic CRUD operations                   |
 * | left join fetch    | Loads each author's books in the same query      |
 * | distinct           | Removes repeated author rows from the join       |
 * | findAllWithBooks() | Gives the service the fetch-planned author query |
 */
public interface AuthorRepository extends JpaRepository<Author, Long> {

    @Query("select distinct a from Author a left join fetch a.books")
    List<Author> findAllWithBooks();
}

/*
 ^ TUTORIAL 10 — nothing custom needed; findAllById covers genre
 * lookups when creating/updating books.
*/
package com.example.bookshop.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bookshop.model.Genre;

/**
 * Standard database access for genres.
 *
 * | Key          | Why we use it                                       |
 * |--------------|-----------------------------------------------------|
 * | JpaRepository| Supplies CRUD and findAllById without a custom query|
 */
public interface GenreRepository extends JpaRepository<Genre, Long> {
}

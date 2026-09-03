/*
 ^ TUTORIAL 10 — nothing custom needed; findAllById covers genre
 * lookups when creating/updating books.
*/
package com.example.bookshop.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bookshop.model.Genre;

public interface GenreRepository extends JpaRepository<Genre, Long> {
}

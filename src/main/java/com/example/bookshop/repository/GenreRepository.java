package com.example.bookshop.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bookshop.model.Genre;

/**
 * Repository for Genre entities.
 *
 * | Key                        | Why we use it                                  |
 * |----------------------------|------------------------------------------------|
 * | JpaRepository<Genre, Long> | Supplies findAllById for resolving request ids |
 */
public interface GenreRepository extends JpaRepository<Genre, Long> {
}

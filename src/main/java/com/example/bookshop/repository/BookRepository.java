package com.example.bookshop.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bookshop.model.Book;

/**
 * Spring Data creates this repository implementation at startup.
 *
 * | Key                       | Why we use it                                  |
 * |---------------------------|------------------------------------------------|
 * | JpaRepository<Book, Long> | Supplies CRUD methods for Book and its Long id |
 */
public interface BookRepository extends JpaRepository<Book, Long> {
}

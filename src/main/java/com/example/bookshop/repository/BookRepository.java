package com.example.bookshop.repository;

import java.util.List;

import com.example.bookshop.model.Book;

/**
 * Repository boundary for book data at this tutorial stage.
 *
 * | Key           | Why we use it                                      |
 * |---------------|----------------------------------------------------|
 * | interface     | Declares data operations without handling HTTP     |
 * | getAllBooks() | Gives the service a method to request all books     |
 */
public interface BookRepository {
    List<Book> getAllBooks();
}

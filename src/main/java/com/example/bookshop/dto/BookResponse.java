package com.example.bookshop.dto;

import java.math.BigDecimal;

import com.example.bookshop.model.Book;

/**
 * Allow-listed public representation of a book.
 *
 * | Key        | Why we use it                                      |
 * |------------|----------------------------------------------------|
 * | record     | Defines an immutable response data carrier         |
 * | from(...) | Maps only selected entity fields into the response  |
 * | no costPrice | Prevents leaking internal cost to API clients    |
 */
public record BookResponse(Long id, String title, String author, BigDecimal price) {

    public static BookResponse from(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getPrice());
    }
}

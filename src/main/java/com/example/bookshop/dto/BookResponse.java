package com.example.bookshop.dto;

import java.math.BigDecimal;
import java.util.List;

import com.example.bookshop.model.Book;
import com.example.bookshop.model.Genre;

/**
 * Allow-listed public representation of a book.
 *
 * | Key                 | Why we use it                              |
 * |---------------------|--------------------------------------------|
 * | record              | Defines an immutable response data carrier |
 * | AuthorSummary       | Returns just an author's id and name       |
 * | List<String> genres | Returns readable genre names, not entities |
 * | from(Book)          | Maps the entity into an API-safe response  |
 * | no costPrice        | Prevents leaking internal cost to clients  |
 */
public record BookResponse(Long id, String title, AuthorSummary author,
        List<String> genres, BigDecimal price) {

    public record AuthorSummary(Long id, String name) {
    }

    public static BookResponse from(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                new AuthorSummary(book.getAuthor().getId(), book.getAuthor().getName()),
                book.getGenres().stream().map(Genre::getName).sorted().toList(),
                book.getPrice());
    }
}

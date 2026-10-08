package com.example.bookshop.dto;

import java.util.List;

import com.example.bookshop.model.Author;
import com.example.bookshop.model.Book;

/**
 * Response DTO for an author with their associated book titles.
 *
 * | Key          | Why we use it                              |
 * |--------------|--------------------------------------------|
 * | id / name    | Identifies and labels the author           |
 * | books        | Returns the author's book titles as a list |
 * | from(Author) | Maps the entity into the response DTO      |
 */
public record AuthorResponse(Long id, String name, List<String> books) {

    public static AuthorResponse from(Author author) {
        return new AuthorResponse(author.getId(), author.getName(),
                author.getBooks().stream().map(Book::getTitle).sorted().toList());
    }
}

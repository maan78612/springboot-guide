package com.example.bookshop.dto;

import java.util.List;

import com.example.bookshop.model.Author;
import com.example.bookshop.model.Book;

public record AuthorResponse(Long id, String name, List<String> books) {

    public static AuthorResponse from(Author author) {
        return new AuthorResponse(author.getId(), author.getName(),
                author.getBooks().stream().map(Book::getTitle).sorted().toList());
    }
}

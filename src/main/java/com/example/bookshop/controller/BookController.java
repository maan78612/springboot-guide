package com.example.bookshop.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bookshop.model.Book;

/**
 * First REST endpoint for listing books.
 *
 * | Method | Endpoint       | Status | Description          |
 * |--------|----------------|--------|----------------------|
 * | GET    | /api/v1/books  | 200    | Return sample books  |
 *
 * | Key             | Explanation                                      |
 * |-----------------|--------------------------------------------------|
 * | @RestController | Handles HTTP requests and serializes return data |
 * | @RequestMapping | Sets the shared URL prefix                       |
 * | @GetMapping     | Maps GET requests to the method                  |
 */
@RestController
@RequestMapping("/api/v1/books")
public class BookController {

    @GetMapping
    public List<Book> getAllBooks() {
        return List.of(
                new Book(1L, "Effective Java", "Joshua Bloch", new BigDecimal("54.99")),
                new Book(2L, "Clean Code", "Robert C. Martin", new BigDecimal("42.50"))
        );
    }
}

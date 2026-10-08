package com.example.bookshop.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bookshop.dto.ApiResponse;
import com.example.bookshop.dto.AuthorResponse;
import com.example.bookshop.dto.BookResponse;
import com.example.bookshop.dto.DiscountRequest;
import com.example.bookshop.service.AuthorService;
import com.example.bookshop.service.BookService;

import jakarta.validation.Valid;

/**
 * Lists authors and applies bulk discounts to an author's books.
 *
 * | Method | Endpoint                      | Status | Description                              |
 * |--------|-------------------------------|--------|------------------------------------------|
 * | GET    | /api/v1/authors               | 200    | Lists authors and their book titles      |
 * | POST   | /api/v1/authors/{id}/discount | 200    | Applies bulk discount to author's books  |
 *
 * | Key             | Why we use it                                      |
 * |-----------------|----------------------------------------------------|
 * | @RestController | Returns the response as JSON                       |
 * | @RequestMapping | Sets the shared `/api/v1/authors` URL prefix       |
 * | @Valid          | Validates DiscountRequest before method execution  |
 * | BookResponse    | Formats discounted books without exposing entities |
 */
@RestController
@RequestMapping("/api/v1/authors")
public class AuthorController {

    private final AuthorService authorService;
    private final BookService bookService;

    public AuthorController(AuthorService authorService, BookService bookService) {
        this.authorService = authorService;
        this.bookService = bookService;
    }

    @GetMapping
    public ApiResponse<List<AuthorResponse>> getAllAuthors() {
        List<AuthorResponse> authors = authorService.getAllAuthors().stream()
                .map(AuthorResponse::from)
                .toList();

        return ApiResponse.ok("Authors fetched", authors);
    }

    @PostMapping("/{id}/discount")
    public ApiResponse<List<BookResponse>> applyDiscount(
            @PathVariable Long id,
            @Valid @RequestBody DiscountRequest request) {
        List<BookResponse> books = bookService.applyAuthorDiscount(id, request.percent()).stream()
                .map(BookResponse::from)
                .toList();

        return ApiResponse.ok("Discount applied", books);
    }
}

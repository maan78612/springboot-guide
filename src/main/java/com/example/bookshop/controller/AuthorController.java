package com.example.bookshop.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bookshop.dto.ApiResponse;
import com.example.bookshop.dto.AuthorResponse;
import com.example.bookshop.service.AuthorService;

/**
 * Lists authors and their book titles.
 *
 * | Key             | Why we use it                                      |
 * |-----------------|----------------------------------------------------|
 * | @RestController | Returns the response as JSON                       |
 * | @RequestMapping | Sets the shared `/api/v1/authors` URL prefix       |
 * | @GetMapping     | Maps GET requests to the author-list method       |
 * | AuthorResponse  | Keeps JPA entity objects out of the API            |
 */
@RestController
@RequestMapping("/api/v1/authors")
public class AuthorController {

    private final AuthorService authorService;

    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @GetMapping
    public ApiResponse<List<AuthorResponse>> getAllAuthors() {
        List<AuthorResponse> authors = authorService.getAllAuthors().stream()
                .map(AuthorResponse::from)
                .toList();

        return ApiResponse.ok("Authors fetched", authors);
    }
}

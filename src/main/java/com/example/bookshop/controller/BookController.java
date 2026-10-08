package com.example.bookshop.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bookshop.dto.ApiResponse;
import com.example.bookshop.dto.BookRequest;
import com.example.bookshop.dto.BookResponse;
import com.example.bookshop.model.Book;
import com.example.bookshop.service.BookService;

/**
 * Book REST endpoints using request/response DTOs and a response envelope.
 *
 * | Method | Endpoint             | Status | Description                    |
 * |--------|----------------------|--------|--------------------------------|
 * | GET    | /api/v1/books        | 200    | Return enveloped book list     |
 * | GET    | /api/v1/books/{id}   | 200    | Return one enveloped book      |
 * | POST   | /api/v1/books        | 201    | Create; include Location       |
 * | PUT    | /api/v1/books/{id}   | 200    | Update a book                  |
 * | DELETE | /api/v1/books/{id}   | 200    | Delete a book                  |
 *
 * | Key                | Explanation                                      |
 * |--------------------|--------------------------------------------------|
 * | @RequestBody       | Converts JSON into BookRequest                   |
 * | @PathVariable      | Binds {id} from the URL to a method parameter    |
 * | ResponseEntity     | Sets create status and Location header           |
 * | ApiResponse        | Gives responses a consistent JSON body           |
 */
@RestController
@RequestMapping("/api/v1/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public ApiResponse<List<BookResponse>> getAllBooks() {
        List<BookResponse> books = bookService.getAllBooks().stream()
                .map(BookResponse::from)
                .toList();

        return ApiResponse.ok("Books fetched", books);
    }

    @GetMapping("/{id}")
    public ApiResponse<BookResponse> getBookById(@PathVariable Long id) {
        BookResponse book = BookResponse.from(bookService.getBookById(id));

        return ApiResponse.ok("Book fetched", book);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BookResponse>> createBook(@RequestBody BookRequest request) {
        Book saved = bookService.createBook(request);
        URI location = URI.create("/api/v1/books/" + saved.getId());

        return ResponseEntity.created(location)
                .body(ApiResponse.ok("Book created", BookResponse.from(saved)));
    }

    @PutMapping("/{id}")
    public ApiResponse<BookResponse> updateBook(@PathVariable Long id,
            @RequestBody BookRequest request) {
        return ApiResponse.ok("Book updated",
                BookResponse.from(bookService.updateBook(id, request)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);

        return ApiResponse.ok("Book deleted", null);
    }
}

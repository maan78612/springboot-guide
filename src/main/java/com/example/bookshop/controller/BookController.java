package com.example.bookshop.controller;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.bookshop.dto.ApiResponse;
import com.example.bookshop.dto.BookRequest;
import com.example.bookshop.dto.BookResponse;
import com.example.bookshop.dto.PageMeta;
import com.example.bookshop.model.Book;
import com.example.bookshop.service.BookService;

import jakarta.validation.Valid;

/**
 * REST endpoints for creating, reading, updating, and deleting books with pagination and filtering.
 *
 * | Method | Endpoint           | Status | Description                                |
 * |--------|--------------------|--------|--------------------------------------------|
 * | GET    | /api/v1/books      | 200    | List books with pagination and search/sort |
 * | GET    | /api/v1/books/{id} | 200    | Fetch one book by ID                       |
 * | POST   | /api/v1/books      | 201    | Validate and create; return Location header|
 * | PUT    | /api/v1/books/{id} | 200    | Validate and update a book                 |
 * | DELETE | /api/v1/books/{id} | 200    | Delete a book                              |
 *
 * | Key            | Explanation                                               |
 * |----------------|-----------------------------------------------------------|
 * | @RestController| Handles HTTP requests and serializes return data to JSON  |
 * | @RequestMapping| Sets the shared URL prefix                                |
 * | @RequestBody   | Converts JSON into BookRequest                            |
 * | @PathVariable  | Binds {id} from the URL to a method parameter             |
 * | @RequestParam  | Reads optional filters and paging from the query string   |
 * | @Valid         | Validates BookRequest constraints before method execution |
 * | ResponseEntity | Sets create status and Location header                    |
 * | ApiResponse    | Wraps response data in a standard envelope                |
 */
@RestController
@RequestMapping("/api/v1/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public ApiResponse<List<BookResponse>> getAllBooks(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) Long genreId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) Integer limit) {
        Page<Book> result = bookService.getBooks(
                search, authorId, genreId, minPrice, maxPrice, sort, page, limit);
        List<BookResponse> books = result.getContent().stream()
                .map(BookResponse::from)
                .toList();

        return ApiResponse.ok("Books fetched", books, PageMeta.from(result));
    }

    @GetMapping("/{id}")
    public ApiResponse<BookResponse> getBookById(@PathVariable Long id) {
        BookResponse book = BookResponse.from(bookService.getBookById(id));

        return ApiResponse.ok("Book fetched", book);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BookResponse>> createBook(
            @Valid @RequestBody BookRequest request) {
        Book saved = bookService.createBook(request);
        URI location = URI.create("/api/v1/books/" + saved.getId());

        return ResponseEntity.created(location)
                .body(ApiResponse.ok("Book created", BookResponse.from(saved)));
    }

    @PutMapping("/{id}")
    public ApiResponse<BookResponse> updateBook(
            @PathVariable Long id,
            @Valid @RequestBody BookRequest request) {
        return ApiResponse.ok("Book updated",
                BookResponse.from(bookService.updateBook(id, request)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);

        return ApiResponse.ok("Book deleted", null);
    }
}

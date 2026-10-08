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

import com.example.bookshop.model.Book;
import com.example.bookshop.service.BookService;

/**
 * REST endpoints for creating, reading, updating, and deleting books.
 *
 * | Method | Endpoint             | Status | Description                        |
 * |--------|----------------------|--------|------------------------------------|
 * | GET    | /api/v1/books        | 200    | List all books                     |
 * | GET    | /api/v1/books/{id}   | 200    | Fetch one book                     |
 * | POST   | /api/v1/books        | 201    | Create; return Location header     |
 * | PUT    | /api/v1/books/{id}   | 200    | Update a book                      |
 * | DELETE | /api/v1/books/{id}   | 204    | Delete a book                      |
 *
 * | Key                | Explanation                                      |
 * |--------------------|--------------------------------------------------|
 * | @RequestBody       | Converts JSON into a Book                        |
 * | @PathVariable      | Binds {id} from the URL to a method parameter    |
 * | ResponseEntity     | Sets create/delete status and create Location    |
 */
@RestController
@RequestMapping("/api/v1/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public List<Book> getAllBooks() {
        return bookService.getAllBooks();
    }

    @GetMapping("/{id}")
    public Book getBookById(@PathVariable Long id) {
        return bookService.getBookById(id);
    }

    @PostMapping
    public ResponseEntity<Book> createBook(@RequestBody Book book) {
        Book saved = bookService.createBook(book);
        URI location = URI.create("/api/v1/books/" + saved.getId());

        return ResponseEntity.created(location)
                .body(saved);
    }

    @PutMapping("/{id}")
    public Book updateBook(@PathVariable Long id, @RequestBody Book changes) {
        return bookService.updateBook(id, changes);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
}

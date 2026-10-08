package com.example.bookshop.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.bookshop.model.Book;
import com.example.bookshop.repository.BookRepository;

/**
 * Book CRUD operations between the controller and repository.
 *
 * | Key          | Why we use it                                  |
 * |--------------|------------------------------------------------|
 * | @Service     | Registers business logic in the Spring context |
 * | findById     | Loads an existing book before update/delete     |
 * | save         | Inserts a new book or persists changes          |
 * | delete       | Removes the selected book at this stage         |
 */
@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public Book getBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found: " + id));
    }

    public Book createBook(Book book) {
        return bookRepository.save(book);
    }

    public Book updateBook(Long id, Book changes) {
        Book book = getBookById(id);
        book.setTitle(changes.getTitle());
        book.setAuthor(changes.getAuthor());
        book.setPrice(changes.getPrice());
        return bookRepository.save(book);
    }

    public void deleteBook(Long id) {
        Book book = getBookById(id);
        bookRepository.delete(book);
    }
}

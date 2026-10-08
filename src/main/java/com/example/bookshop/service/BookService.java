package com.example.bookshop.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.bookshop.model.Book;
import com.example.bookshop.repository.BookRepository;

/**
 * Service layer that delegates book reads to the repository.
 *
 * | Key                    | Why we use it                                   |
 * |------------------------|-------------------------------------------------|
 * | @Service               | Registers this class as application logic       |
 * | final repository field | Makes the dependency required and immutable     |
 * | constructor injection  | Lets Spring provide the repository              |
 */
@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<Book> getAllBooks() {
        return bookRepository.getAllBooks();
    }
}

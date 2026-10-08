package com.example.bookshop.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.bookshop.model.Author;
import com.example.bookshop.repository.AuthorRepository;

/**
 * Service for author domain operations.
 *
 * | Key                   | Why we use it                                       |
 * |-----------------------|-----------------------------------------------------|
 * | @Service              | Registers business logic for dependency injection   |
 * | constructor injection | Supplies the repository without manual construction |
 * | findAllWithBooks()    | Avoids per-author lazy queries during mapping       |
 */
@Service
public class AuthorService {

    private final AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public List<Author> getAllAuthors() {
        return authorRepository.findAllWithBooks();
    }
}

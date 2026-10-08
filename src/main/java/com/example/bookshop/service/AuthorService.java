package com.example.bookshop.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * | log (SLF4J)           | Logs service operations and debug diagnostics       |
 * | findAllWithBooks()    | Avoids per-author lazy queries during mapping       |
 */
@Service
public class AuthorService {

    private static final Logger log = LoggerFactory.getLogger(AuthorService.class);

    private final AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public List<Author> getAllAuthors() {
        log.debug("Fetching all authors with fetch-joined books");
        return authorRepository.findAllWithBooks();
    }
}


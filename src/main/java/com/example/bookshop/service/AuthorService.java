package com.example.bookshop.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.bookshop.model.Author;
import com.example.bookshop.repository.AuthorRepository;

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

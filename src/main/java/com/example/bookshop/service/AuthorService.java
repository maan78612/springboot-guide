/*
 ^ TUTORIAL 10 — the N+1 demonstration lives here

 * First version used findAll(): 1 query for authors, then one MORE
 * query per author when the mapping touched author.getBooks().
 * With 4 authors that is 5 queries; with 4,000 it is a meltdown.
 * The SQL log caught it red-handed (see the tutorial doc).
 + Fixed by findAllWithBooks(): one fetch-join query, same result.
*/
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

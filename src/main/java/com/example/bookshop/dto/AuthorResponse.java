/*
 ^ TUTORIAL 10 — outbound shape for authors, with their book titles.
 * Mapping a.getBooks() here is what makes the N+1 problem visible
 * (or not - see AuthorRepository.findAllWithBooks).
*/
package com.example.bookshop.dto;

import java.util.List;

import com.example.bookshop.model.Author;
import com.example.bookshop.model.Book;

public record AuthorResponse(Long id, String name, List<String> books) {

	public static AuthorResponse from(Author author) {
		return new AuthorResponse(
				author.getId(),
				author.getName(),
				author.getBooks().stream().map(Book::getTitle).sorted().toList());
	}
}

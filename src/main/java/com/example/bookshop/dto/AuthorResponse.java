/*
 ^ TUTORIAL 10 — outbound shape for authors, with their book titles.
 * Mapping a.getBooks() here is what makes the N+1 problem visible
 * (or not - see AuthorRepository.findAllWithBooks).
*/
package com.example.bookshop.dto;

import java.util.List;

import com.example.bookshop.model.Author;
import com.example.bookshop.model.Book;

/**
 * Public author response with book titles instead of nested entities.
 *
 * | Key        | Why we use it                                      |
 * |------------|----------------------------------------------------|
 * | id, name   | Identifies and labels the author                   |
 * | books      | Returns sorted book titles without entity objects   |
 * | from(...)  | Maps an Author entity to this API response          |
 */
public record AuthorResponse(Long id, String name, List<String> books) {

	public static AuthorResponse from(Author author) {
		return new AuthorResponse(
				author.getId(),
				author.getName(),
				author.getBooks().stream().map(Book::getTitle).sorted().toList());
	}
}

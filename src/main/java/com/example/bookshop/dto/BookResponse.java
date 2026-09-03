/*
 ^ TUTORIAL 07 — what the API is WILLING to show (no costPrice).

 ^ TUTORIAL 10 — related data is FLATTENED for the client

 ? The entity holds an Author object and a Set<Genre>; the response
 ? shows a small {id, name} summary and a sorted list of genre
 ? names. Clients get what they render, not our object graph -
 ? and the mapping touching author/genres is what triggers their
 ? lazy loading (deliberately, inside the request).
*/
package com.example.bookshop.dto;

import java.math.BigDecimal;
import java.util.List;

import com.example.bookshop.model.Book;
import com.example.bookshop.model.Genre;

public record BookResponse(Long id, String title, AuthorSummary author,
		List<String> genres, BigDecimal price) {

	public record AuthorSummary(Long id, String name) {
	}

	public static BookResponse from(Book book) {
		return new BookResponse(
				book.getId(),
				book.getTitle(),
				new AuthorSummary(book.getAuthor().getId(), book.getAuthor().getName()),
				book.getGenres().stream().map(Genre::getName).sorted().toList(),
				book.getPrice());
	}
}

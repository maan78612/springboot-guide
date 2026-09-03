/*
 ^ TUTORIAL 10 — authors endpoint

 ? GET /api/v1/authors — every author with their book titles.
 * Same pattern as BookController: delegate, map to DTO, wrap in the
 * envelope. The interesting part of that stage is in the repository
 * and the SQL log (N+1 and its fix).

 ^ TUTORIAL 12 — POST /api/v1/authors/{id}/discount
 * Bulk price change - several writes as one transaction. The rules
 * and the rollback behavior live in BookService; a controller may
 * depend on more than one service.
*/
package com.example.bookshop.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bookshop.dto.ApiResponse;
import com.example.bookshop.dto.AuthorResponse;
import com.example.bookshop.dto.BookResponse;
import com.example.bookshop.dto.DiscountRequest;
import com.example.bookshop.service.AuthorService;
import com.example.bookshop.service.BookService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/authors")
public class AuthorController {

	private final AuthorService authorService;
	private final BookService bookService;

	public AuthorController(AuthorService authorService, BookService bookService) {
		this.authorService = authorService;
		this.bookService = bookService;
	}

	@GetMapping
	public ApiResponse<List<AuthorResponse>> getAllAuthors() {
		List<AuthorResponse> authors = authorService.getAllAuthors().stream()
				.map(AuthorResponse::from)
				.toList();
		return ApiResponse.ok("Authors fetched", authors);
	}

	@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/{id}/discount")
	public ApiResponse<List<BookResponse>> applyDiscount(@PathVariable Long id,
			@Valid @RequestBody DiscountRequest request) {
		List<BookResponse> books = bookService.applyAuthorDiscount(id, request.percent()).stream()
				.map(BookResponse::from)
				.toList();
		return ApiResponse.ok("Discount applied", books);
	}
}

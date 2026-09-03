/*
 ^ TUTORIAL 02 — The first endpoint
 ? @RestController: manage this class; method return values are the
 ?   JSON body. @RequestMapping: shared URL prefix. @GetMapping: run
 ?   this method for GET. The framework calls you.

 ^ TUTORIAL 03 — the controller delegates
 * Only HTTP work here: match the request, call the service, choose
 *   the status. Data and rules live below.

 ^ TUTORIAL 06 — the full CRUD surface
 ? GET list/one, POST create (201 + Location), PUT replace,
 ?   DELETE remove. @PathVariable reads the URL, @RequestBody reads
 ?   the JSON body.
 ! Forget @RequestBody -> parameter built empty, all fields null,
 !   saved with a 201. Silent. Check the annotation first.

 ^ TUTORIAL 07 — entities stop at this boundary
 ? In: BookRequest (@Valid since tutorial 08). Out: BookResponse
 ?   inside the {success, message, data, meta} envelope.

 ^ TUTORIAL 09 — the error paths left this file

 * No more Optional-to-404 plumbing: the service throws, the global
 *   handler answers. A controller method now reads as one sentence:
 *   take input, call service, wrap result.
 * ResponseEntity survives only where the SUCCESS status is special:
 *   POST answers 201 + Location. Everything else returns the
 *   envelope directly and lets Spring default to 200.

 ^ TUTORIAL 16 — who is calling matters now
 ? Write endpoints take @AuthenticationPrincipal Jwt and hand the
 ?   token's subject (email) to the service, which loads the account
 ?   and enforces ownership. @PreAuthorize("hasRole('ADMIN')") locks
 ?   the soft-delete admin tools at the METHOD level - on top of the
 ?   URL rule in SecurityConfig (defense in depth).

 ^ TUTORIAL 10 — the list endpoint learns query features

 ? @RequestParam reads "?key=value" from the URL (query string):
 ?   GET /api/v1/books?search=clean&sort=-price&page=1&limit=5
 ? required = false -> the parameter may be absent (null).
 ? defaultValue     -> used when absent.
 * The controller only COLLECTS the knobs. Clamping, whitelisting
 *   and querying are service work. meta appears in the envelope for
 *   the first time, built from the returned Page.
*/
package com.example.bookshop.controller;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.bookshop.dto.ApiResponse;
import com.example.bookshop.dto.BookRequest;
import com.example.bookshop.dto.BookResponse;
import com.example.bookshop.dto.PageMeta;
import com.example.bookshop.model.Book;
import com.example.bookshop.service.BookService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/books")
public class BookController {

	private final BookService bookService;

	public BookController(BookService bookService) {
		this.bookService = bookService;
	}

	@GetMapping
	public ApiResponse<List<BookResponse>> getAllBooks(
			@RequestParam(required = false) String search,
			@RequestParam(required = false) Long authorId,
			@RequestParam(required = false) Long genreId,
			@RequestParam(required = false) BigDecimal minPrice,
			@RequestParam(required = false) BigDecimal maxPrice,
			@RequestParam(required = false) String sort,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(required = false) Integer limit) {
		Page<Book> result = bookService.getBooks(search, authorId, genreId,
				minPrice, maxPrice, sort, page, limit);
		List<BookResponse> books = result.getContent().stream()
				.map(BookResponse::from)
				.toList();
		return ApiResponse.ok("Books fetched", books, PageMeta.from(result));
	}

	@GetMapping("/{id}")
	public ApiResponse<BookResponse> getBookById(@PathVariable Long id) {
		return ApiResponse.ok("Book fetched", BookResponse.from(bookService.getBookById(id)));
	}

	@PostMapping
	public ResponseEntity<ApiResponse<BookResponse>> createBook(@Valid @RequestBody BookRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		Book saved = bookService.createBook(request, jwt.getSubject());
		return ResponseEntity
				.created(URI.create("/api/v1/books/" + saved.getId()))
				.body(ApiResponse.ok("Book created", BookResponse.from(saved)));
	}

	@PutMapping("/{id}")
	public ApiResponse<BookResponse> updateBook(@PathVariable Long id,
			@Valid @RequestBody BookRequest request, @AuthenticationPrincipal Jwt jwt) {
		return ApiResponse.ok("Book updated",
				BookResponse.from(bookService.updateBook(id, request, jwt.getSubject())));
	}

	@DeleteMapping("/{id}")
	public ApiResponse<Void> deleteBook(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
		bookService.deleteBook(id, jwt.getSubject());
		return ApiResponse.ok("Book deleted", null);
	}

	// TUTORIAL 11 — the soft-delete extras.
	// "/deleted" is a literal path, so Spring matches it BEFORE the
	// "/{id}" template - no clash. Both become admin-only in tut. 16.
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/deleted")
	public ApiResponse<List<BookResponse>> getDeletedBooks() {
		List<BookResponse> books = bookService.getDeletedBooks().stream()
				.map(BookResponse::from)
				.toList();
		return ApiResponse.ok("Deleted books fetched", books);
	}

	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/{id}/restore")
	public ApiResponse<BookResponse> restoreBook(@PathVariable Long id) {
		return ApiResponse.ok("Book restored", BookResponse.from(bookService.restoreBook(id)));
	}
}

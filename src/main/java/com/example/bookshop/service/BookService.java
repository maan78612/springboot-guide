/*
 ^ TUTORIAL 03 — layers + constructor injection: declare needs as
 ?   final fields, container wires them. Never `new` a layer.

 ^ TUTORIAL 06 — CRUD. save(): no id -> INSERT, id -> UPDATE.

 ^ TUTORIAL 07 — inbound DTO -> entity mapping happens here.

 ^ TUTORIAL 09 — "not found" throws ApiException; the global handler
 ?   turns it into the standard 404 JSON.

 ^ TUTORIAL 10 — real business logic at last

 ? getBooks() is the queryFeatures port from the node reference:
 ?   every filter optional, page 1-based, limit clamped to the
 ?   configured maximum (tutorial 04's BookshopProperties earning
 ?   its keep), sort WHITELISTED via SORTABLE_FIELDS.
 + Whitelisting beats blacklisting: clients sort by what we allow
 +   ("price", "-price,title"), unknown fields are dropped silently,
 +   and nothing internal (costPrice!) can be probed via sorting.

 ? Business rules live here, not in the controller and not in
 ? annotations:
 ?   duplicate title per author  -> 409 conflict
 ?   unknown authorId / genreId  -> 400 bad request

 ^ TUTORIAL 16 — ownership checks

 ? The controller passes WHO is acting (the token's email); this
 ?   service loads the account and decides. Identity arrives as an
 ?   explicit parameter, not from a hidden static context - that is
 ?   why the unit tests can exercise these rules with plain mocks.
 ? Role comes from the DATABASE row, not the token claim: a demoted
 ?   admin loses power on their next request, not when their token
 ?   expires.
 + Rule: admins modify anything; a seller modifies only books they
 +   own; seeded books (owner = null) are admin-only.
*/
package com.example.bookshop.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bookshop.config.BookshopProperties;
import com.example.bookshop.dto.BookRequest;
import com.example.bookshop.exception.ApiException;
import com.example.bookshop.model.Author;
import com.example.bookshop.model.Book;
import com.example.bookshop.model.Genre;
import com.example.bookshop.repository.AuthorRepository;
import com.example.bookshop.repository.BookRepository;
import com.example.bookshop.repository.GenreRepository;
import com.example.bookshop.repository.UserAccountRepository;
import com.example.bookshop.model.Role;
import com.example.bookshop.model.UserAccount;

@Service
public class BookService {

	/*
	 ^ TUTORIAL 14 — one logger per class, made from the class itself
	 ? SLF4J is the logging FACADE (the API we write against);
	 ? Logback is the engine behind it. static final: one logger per
	 ? class, named after it - the name is how levels are filtered.
	 */
	private static final Logger log = LoggerFactory.getLogger(BookService.class);

	private static final Set<String> SORTABLE_FIELDS = Set.of("id", "title", "price");

	private final BookRepository bookRepository;
	private final AuthorRepository authorRepository;
	private final GenreRepository genreRepository;
	private final UserAccountRepository userRepository;
	private final BookshopProperties properties;

	public BookService(BookRepository bookRepository, AuthorRepository authorRepository,
			GenreRepository genreRepository, UserAccountRepository userRepository,
			BookshopProperties properties) {
		this.bookRepository = bookRepository;
		this.authorRepository = authorRepository;
		this.genreRepository = genreRepository;
		this.userRepository = userRepository;
		this.properties = properties;
	}

	public Page<Book> getBooks(String search, Long authorId, Long genreId,
			BigDecimal minPrice, BigDecimal maxPrice, String sort, int page, Integer limit) {
		int pageIndex = Math.max(page, 1) - 1;
		int defaultSize = properties.getCatalog().getDefaultPageSize();
		int maxSize = properties.getCatalog().getMaxPageSize();
		int size = (limit == null) ? defaultSize : Math.min(Math.max(limit, 1), maxSize);
		Pageable pageable = PageRequest.of(pageIndex, size, parseSort(sort));
		String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim();
		// {} placeholders, never string +: the message is only built if
		// the level is on, and values stay visually separate from text.
		log.debug("Book search: search={}, authorId={}, genreId={}, page={}, size={}",
				normalizedSearch, authorId, genreId, pageIndex + 1, size);
		return bookRepository.search(normalizedSearch, authorId, genreId, minPrice, maxPrice, pageable);
	}

	// "?sort=-price,title" -> price DESC, then title ASC.
	// Unknown fields are dropped; empty result falls back to newest-first.
	private Sort parseSort(String sort) {
		if (sort == null || sort.isBlank()) {
			return Sort.by(Sort.Direction.DESC, "id");
		}
		List<Sort.Order> orders = new ArrayList<>();
		for (String part : sort.split(",")) {
			String trimmed = part.trim();
			boolean descending = trimmed.startsWith("-");
			String field = descending ? trimmed.substring(1) : trimmed;
			if (SORTABLE_FIELDS.contains(field)) {
				orders.add(descending ? Sort.Order.desc(field) : Sort.Order.asc(field));
			}
		}
		return orders.isEmpty() ? Sort.by(Sort.Direction.DESC, "id") : Sort.by(orders);
	}

	public Book getBookById(Long id) {
		return bookRepository.findById(id)
				.orElseThrow(() -> ApiException.notFound("Book with id " + id + " not found"));
	}

	public Book createBook(BookRequest request, String ownerEmail) {
		if (bookRepository.existsByTitleIgnoreCaseAndAuthorId(request.title(), request.authorId())) {
			throw ApiException.conflict("This author already has a book titled '" + request.title() + "'");
		}
		Book book = new Book(request.title(), resolveAuthor(request.authorId()), request.price());
		book.setGenres(resolveGenres(request.genreIds()));
		book.setOwner(actor(ownerEmail));
		Book saved = bookRepository.save(book);
		log.info("Book created: id={}, authorId={}", saved.getId(), request.authorId());
		return saved;
	}

	public Book updateBook(Long id, BookRequest changes, String actorEmail) {
		Book book = getBookById(id);
		assertCanModify(book, actor(actorEmail));
		if (bookRepository.existsByTitleIgnoreCaseAndAuthorIdAndIdNot(
				changes.title(), changes.authorId(), id)) {
			throw ApiException.conflict("This author already has a book titled '" + changes.title() + "'");
		}
		book.setTitle(changes.title());
		book.setAuthor(resolveAuthor(changes.authorId()));
		book.setGenres(resolveGenres(changes.genreIds()));
		book.setPrice(changes.price());
		return bookRepository.save(book);
	}

	// TUTORIAL 11: unchanged code, changed meaning - @SoftDelete turns
	// this delete() into "UPDATE book SET deleted = true".
	public void deleteBook(Long id, String actorEmail) {
		Book book = getBookById(id);
		assertCanModify(book, actor(actorEmail));
		bookRepository.delete(book);
		log.info("Book soft-deleted: id={}", id);
	}

	public List<Book> getDeletedBooks() {
		return bookRepository.findDeleted();
	}

	// @Transactional: required for a @Modifying query; the full story
	// is tutorial 12.
	@Transactional
	public Book restoreBook(Long id) {
		int restored = bookRepository.restoreById(id);
		if (restored == 0) {
			throw ApiException.notFound("No deleted book with id " + id);
		}
		bookRepository.restoreGenreLinks(id);
		log.info("Book restored: id={}", id);
		return getBookById(id);
	}

	/*
	 ^ TUTORIAL 12 — several writes that must live or die together
	 ? Discount every book of one author; rule: a discounted price may
	 ? not drop below the shop's cost for that book.
	 ! Without @Transactional each save() commits on its own - a rule
	 !   violation on book 3 leaves books 1-2 discounted: half-applied
	 !   state, reproduced in the doc.
	 + @Transactional = one unit: any RuntimeException rolls back every
	 +   write the method made. (Checked exceptions do NOT roll back by
	 +   default - also reproduced in the doc.)
	 */
	@Transactional
	public List<Book> applyAuthorDiscount(Long authorId, int percent) {
		if (!authorRepository.existsById(authorId)) {
			throw ApiException.notFound("Author with id " + authorId + " not found");
		}
		BigDecimal factor = BigDecimal.valueOf(100 - percent).movePointLeft(2);
		List<Book> books = bookRepository.findByAuthorId(authorId);
		for (Book book : books) {
			BigDecimal newPrice = book.getPrice().multiply(factor).setScale(2, RoundingMode.HALF_UP);
			if (book.getCostPrice() != null && newPrice.compareTo(book.getCostPrice()) < 0) {
				throw ApiException.conflict("A " + percent + "% discount would push '"
						+ book.getTitle() + "' below its cost price");
			}
			book.setPrice(newPrice);
			bookRepository.save(book);
		}
		log.info("Applied {}% discount to {} books of author {}", percent, books.size(), authorId);
		return books;
	}

	private UserAccount actor(String email) {
		return userRepository.findByEmailIgnoreCase(email)
				.orElseThrow(() -> ApiException.unauthorized("Account no longer exists"));
	}

	private void assertCanModify(Book book, UserAccount actor) {
		if (actor.getRole() == Role.ADMIN) {
			return;
		}
		if (book.getOwner() == null || !book.getOwner().getId().equals(actor.getId())) {
			throw ApiException.forbidden("You can only modify books you created");
		}
	}

	private Author resolveAuthor(Long authorId) {
		return authorRepository.findById(authorId)
				.orElseThrow(() -> ApiException.badRequest("Author with id " + authorId + " does not exist"));
	}

	private Set<Genre> resolveGenres(Set<Long> genreIds) {
		if (genreIds == null || genreIds.isEmpty()) {
			return new HashSet<>();
		}
		List<Genre> found = genreRepository.findAllById(genreIds);
		if (found.size() != genreIds.size()) {
			throw ApiException.badRequest("One or more genre ids do not exist");
		}
		return new HashSet<>(found);
	}
}

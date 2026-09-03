/*
 ^ TUTORIAL 13 — level 1: unit tests (no Spring, no database, no HTTP)

 ? A unit test checks ONE class in isolation. Its collaborators are
 ? replaced with MOCKS: fake objects that answer what we script and
 ? record what was called on them. Mockito makes them.

 ? Why BookService can be tested without Spring at all: constructor
 ? injection (tutorial 03). It is just a class - we construct it by
 ? hand and pass mocks in. No container, so tests run in milliseconds.

 ? The Mockito vocabulary used below:
 ?   @Mock                     make a fake of this type
 ?   when(x).thenReturn(y)     script an answer
 ?   verify(x, never()).m()    assert something did NOT happen
 ?   ArgumentCaptor            catch what a mock was called WITH
 ? and AssertJ for readable assertions: assertThat / assertThatThrownBy.

 ! What a unit test CANNOT see: @Transactional (proxies exist only in
 !   the container), real SQL, JSON shapes. Wrong level for those.
*/
package com.example.bookshop.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import com.example.bookshop.config.BookshopProperties;
import com.example.bookshop.dto.BookRequest;
import com.example.bookshop.exception.ApiException;
import com.example.bookshop.model.Author;
import com.example.bookshop.model.Book;
import com.example.bookshop.repository.AuthorRepository;
import com.example.bookshop.repository.BookRepository;
import com.example.bookshop.repository.GenreRepository;
import com.example.bookshop.repository.UserAccountRepository;
import com.example.bookshop.model.Role;
import com.example.bookshop.model.UserAccount;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

	@Mock
	private BookRepository bookRepository;

	@Mock
	private AuthorRepository authorRepository;

	@Mock
	private GenreRepository genreRepository;

	@Mock
	private UserAccountRepository userRepository;

	private BookService bookService;

	@BeforeEach
	void setUp() {
		// Plain construction - the whole point of constructor injection.
		// BookshopProperties is a simple object; no need to mock it.
		bookService = new BookService(bookRepository, authorRepository,
				genreRepository, userRepository, new BookshopProperties());
	}

	@Test
	void getBookById_throwsNotFound_whenIdUnknown() {
		when(bookRepository.findById(42L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> bookService.getBookById(42L))
				.isInstanceOf(ApiException.class)
				.hasMessage("Book with id 42 not found")
				.extracting(ex -> ((ApiException) ex).getStatus())
				.isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void createBook_rejectsDuplicate_andNeverSaves() {
		when(bookRepository.existsByTitleIgnoreCaseAndAuthorId("Clean Code", 2L))
				.thenReturn(true);

		BookRequest request = new BookRequest("Clean Code", 2L, null, new BigDecimal("10.00"));

		assertThatThrownBy(() -> bookService.createBook(request, "seller@example.com"))
				.isInstanceOf(ApiException.class)
				.extracting(ex -> ((ApiException) ex).getStatus())
				.isEqualTo(HttpStatus.CONFLICT);

		verify(bookRepository, never()).save(any());
	}

	// TUTORIAL 16 — the ownership rules, unit-tested with plain mocks.
	// This is why identity is an explicit parameter instead of a
	// hidden static SecurityContext: no Spring needed to test it.
	@Test
	void updateBook_forbidden_whenActorIsNotTheOwner() {
		UserAccount owner = new UserAccount("Owner", "owner@example.com", "hash", Role.USER);
		ReflectionTestUtils.setField(owner, "id", 5L);
		UserAccount actor = new UserAccount("Other", "other@example.com", "hash", Role.USER);
		ReflectionTestUtils.setField(actor, "id", 2L);
		Book book = new Book("Theirs", new Author("A"), new BigDecimal("10.00"));
		book.setOwner(owner);
		when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
		when(userRepository.findByEmailIgnoreCase("other@example.com"))
				.thenReturn(Optional.of(actor));

		BookRequest changes = new BookRequest("Theirs", 1L, null, new BigDecimal("12.00"));

		assertThatThrownBy(() -> bookService.updateBook(1L, changes, "other@example.com"))
				.isInstanceOf(ApiException.class)
				.extracting(ex -> ((ApiException) ex).getStatus())
				.isEqualTo(HttpStatus.FORBIDDEN);

		verify(bookRepository, never()).save(any());
	}

	@Test
	void deleteBook_allowed_forAdmin_onAnyBook() {
		UserAccount admin = new UserAccount("Admin", "admin@example.com", "hash", Role.ADMIN);
		ReflectionTestUtils.setField(admin, "id", 1L);
		Book book = new Book("House stock", new Author("A"), new BigDecimal("10.00"));
		// owner stays null - seeded book
		when(bookRepository.findById(3L)).thenReturn(Optional.of(book));
		when(userRepository.findByEmailIgnoreCase("admin@example.com"))
				.thenReturn(Optional.of(admin));

		bookService.deleteBook(3L, "admin@example.com");

		verify(bookRepository).delete(book);
	}

	@Test
	void applyAuthorDiscount_discountsEveryBook() {
		Author author = new Author("Kent Beck");
		Book book = new Book("TDD", author, new BigDecimal("50.00"));
		book.setCostPrice(new BigDecimal("20.00"));
		when(authorRepository.existsById(7L)).thenReturn(true);
		when(bookRepository.findByAuthorId(7L)).thenReturn(List.of(book));

		bookService.applyAuthorDiscount(7L, 10);

		assertThat(book.getPrice()).isEqualTo(new BigDecimal("45.00"));
	}

	@Test
	void applyAuthorDiscount_rejectsPriceBelowCost() {
		Author author = new Author("Kent Beck");
		Book book = new Book("TDD", author, new BigDecimal("50.00"));
		book.setCostPrice(new BigDecimal("49.00"));
		when(authorRepository.existsById(7L)).thenReturn(true);
		when(bookRepository.findByAuthorId(7L)).thenReturn(List.of(book));

		assertThatThrownBy(() -> bookService.applyAuthorDiscount(7L, 10))
				.isInstanceOf(ApiException.class)
				.hasMessageContaining("below its cost price");
	}

	@Test
	void getBooks_clampsLimitToConfiguredMaximum() {
		when(bookRepository.search(any(), any(), any(), any(), any(), any(Pageable.class)))
				.thenReturn(Page.empty());

		bookService.getBooks(null, null, null, null, null, null, 1, 5000);

		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(bookRepository).search(any(), any(), any(), any(), any(), pageable.capture());
		assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
	}
}

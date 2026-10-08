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

/**
 * Book service handling business logic, validation, and data mapping.
 *
 * | Key                   | Why we use it                                       |
 * |-----------------------|-----------------------------------------------------|
 * | @Service              | Registers business logic in the Spring context      |
 * | constructor injection | Wires required repositories and properties          |
 * | log (SLF4J)           | Logs domain events and debug diagnostics            |
 * | getBooks              | Implements filtered search with allow-listed sort   |
 * | PageRequest           | Converts page, size, and sort into a pageable query |
 * | SORTABLE_FIELDS       | Whitelists allowed sort fields                      |
 * | resolveAuthor         | Verifies and fetches related Author entity          |
 * | resolveGenres         | Verifies and fetches related Genre entities         |
 * | @Transactional        | Runs bulk discount updates as an atomic transaction |
 */
@Service
public class BookService {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);

    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "title", "price");


    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;
    private final BookshopProperties properties;

    public BookService(BookRepository bookRepository, AuthorRepository authorRepository,
            GenreRepository genreRepository, BookshopProperties properties) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
        this.genreRepository = genreRepository;
        this.properties = properties;
    }

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public Book getBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Book with id " + id + " not found"));
    }

    public Book createBook(BookRequest request) {
        if (bookRepository.existsByTitleIgnoreCaseAndAuthorId(request.title(), request.authorId())) {
            throw ApiException.conflict("This author already has a book titled '" + request.title() + "'");
        }

        Book book = new Book(request.title(), resolveAuthor(request.authorId()), request.price());
        book.setGenres(resolveGenres(request.genreIds()));
        Book saved = bookRepository.save(book);
        log.info("Book created: id={}, title={}, authorId={}", saved.getId(), saved.getTitle(), request.authorId());
        return saved;
    }

    public Book updateBook(Long id, BookRequest changes) {
        Book book = getBookById(id);
        if (bookRepository.existsByTitleIgnoreCaseAndAuthorIdAndIdNot(
                changes.title(), changes.authorId(), id)) {
            throw ApiException.conflict("This author already has a book titled '" + changes.title() + "'");
        }
        book.setTitle(changes.title());
        book.setAuthor(resolveAuthor(changes.authorId()));
        book.setGenres(resolveGenres(changes.genreIds()));
        book.setPrice(changes.price());
        Book saved = bookRepository.save(book);
        log.info("Book updated: id={}, title={}, authorId={}", saved.getId(), saved.getTitle(), changes.authorId());
        return saved;
    }

    public void deleteBook(Long id) {
        Book book = getBookById(id);
        bookRepository.delete(book);
        log.info("Book deleted: id={}", id);
    }

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

    public Page<Book> getBooks(String search, Long authorId, Long genreId,
            BigDecimal minPrice, BigDecimal maxPrice, String sort, int page, Integer limit) {
        int pageIndex = Math.max(page, 1) - 1;
        int defaultSize = properties.getCatalog().getDefaultPageSize();
        int maxSize = properties.getCatalog().getMaxPageSize();
        int size = limit == null ? defaultSize : Math.min(Math.max(limit, 1), maxSize);
        Pageable pageable = PageRequest.of(pageIndex, size, parseSort(sort));
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();

        log.debug("Book search: search={}, authorId={}, genreId={}, minPrice={}, maxPrice={}, page={}, size={}",
                normalizedSearch, authorId, genreId, minPrice, maxPrice, pageIndex + 1, size);

        return bookRepository.search(normalizedSearch, authorId, genreId,
                minPrice, maxPrice, pageable);
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "id");
        }

        List<Sort.Order> orders = new ArrayList<>();
        for (String part : sort.split(",")) {
            String value = part.trim();
            boolean descending = value.startsWith("-");
            String field = descending ? value.substring(1) : value;
            if (SORTABLE_FIELDS.contains(field)) {
                orders.add(descending ? Sort.Order.desc(field) : Sort.Order.asc(field));
            }
        }
        return orders.isEmpty() ? Sort.by(Sort.Direction.DESC, "id") : Sort.by(orders);
    }

    private Author resolveAuthor(Long authorId) {
        return authorRepository.findById(authorId)
                .orElseThrow(() -> ApiException.badRequest(
                        "Author with id " + authorId + " does not exist"));
    }

    private Set<Genre> resolveGenres(Set<Long> genreIds) {
        if (genreIds == null || genreIds.isEmpty()) {
            return new HashSet<>();
        }

        List<Genre> genres = genreRepository.findAllById(genreIds);
        if (genres.size() != genreIds.size()) {
            throw ApiException.badRequest("One or more genre ids do not exist");
        }
        return new HashSet<>(genres);
    }
}

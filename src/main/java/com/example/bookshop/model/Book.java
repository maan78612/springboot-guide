package com.example.bookshop.model;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.BatchSize;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

/**
 * JPA mapping from the Book class to a database row.
 *
 * | Key / Annotation          | Why we use it                                     |
 * |----------------------------|--------------------------------------------------|
 * | @Entity                    | Maps Book objects to database rows               |
 * | @ManyToOne                 | Many books can reference one author              |
 * | FetchType.LAZY             | Loads the author when the application accesses it|
 * | optional = false           | Requires each book to have an author             |
 * | @JoinColumn(author_id)     | Stores the author foreign key on the book row    |
 * | @ManyToMany                | Allows each book to have multiple genres         |
 * | @BatchSize(50)             | Batches lazy genre loading for up to 50 books    |
 * | @JoinTable(book_genre)     | Stores book/genre links in a separate table      |
 * | joinColumns                | Names the link table column for the Book side    |
 * | inverseJoinColumns         | Names the link table column for the Genre side   |
 * | Set / HashSet              | Avoids duplicate genres and starts non-null      |
 * | @SoftDelete(columnName=...) | Marks rows instead of physically removing them   |
 */
@Entity
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private Author author;

    @ManyToMany(fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    @JoinTable(name = "book_genre", joinColumns = @JoinColumn(name = "book_id"), inverseJoinColumns = @JoinColumn(name = "genre_id"))
    private Set<Genre> genres = new HashSet<>();

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(precision = 10, scale = 2)
    private BigDecimal costPrice;

    protected Book() {
    }

    public Book(String title, Author author, BigDecimal price) {
        this.title = title;
        this.author = author;
        this.price = price;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }

    public Set<Genre> getGenres() {
        return genres;
    }

    public void setGenres(Set<Genre> genres) {
        this.genres = genres;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }
}

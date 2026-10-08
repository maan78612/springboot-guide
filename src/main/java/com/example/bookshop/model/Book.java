package com.example.bookshop.model;

import java.math.BigDecimal;

/**
 * Plain Java model returned by the first endpoint.
 *
 * | Key            | Why we use it                                      |
 * |----------------|----------------------------------------------------|
 * | BigDecimal     | Stores money without floating-point rounding       |
 * | private fields | Keeps object state accessed through public methods |
 * | public getters | Lets Jackson read values and serialize JSON        |
 */
public class Book {

    private Long id;
    private String title;
    private String author;
    private BigDecimal price;

    public Book(Long id, String title, String author, BigDecimal price) {
        this.id = id;
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

    public String getAuthor() {
        return author;
    }

    public BigDecimal getPrice() {
        return price;
    }
}

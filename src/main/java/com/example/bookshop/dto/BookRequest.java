package com.example.bookshop.dto;

import java.math.BigDecimal;

/**
 * Allow-listed fields clients may provide when writing a book.
 *
 * | Key          | Why we use it                                     |
 * |--------------|---------------------------------------------------|
 * | title/author | Client-editable book details                      |
 * | price        | Client-provided selling price                     |
 * | no id        | Database assigns the primary key                  |
 * | no costPrice | Keeps internal shop cost out of requests          |
 */
public record BookRequest(String title, String author, BigDecimal price) {
}

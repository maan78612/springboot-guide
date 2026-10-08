package com.example.bookshop.dto;

import org.springframework.data.domain.Page;

/**
 * Pagination metadata carried in the ApiResponse envelope.
 *
 * | Key                   | Why we use it                                           |
 * |-----------------------|---------------------------------------------------------|
 * | Page<?>               | Provides total counts and current page information      |
 * | getNumber() + 1       | Converts Spring's zero-based page to one-based API page |
 * | hasNext / hasPrevious | Lets clients enable paging controls                     |
 */
public record PageMeta(long total, int page, int limit, int totalPages,
        boolean hasNextPage, boolean hasPrevPage) {

    public static PageMeta from(Page<?> result) {
        return new PageMeta(
                result.getTotalElements(),
                result.getNumber() + 1,
                result.getSize(),
                Math.max(result.getTotalPages(), 1),
                result.hasNext(),
                result.hasPrevious());
    }
}

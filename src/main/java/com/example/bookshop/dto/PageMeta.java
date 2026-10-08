package com.example.bookshop.dto;

import org.springframework.data.domain.Page;

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

package com.example.bookshop.dto;

import java.math.BigDecimal;
import java.util.Set;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record BookRequest(
        @NotBlank(message = "title is required")
        @Size(max = 200, message = "title must be at most 200 characters")
        String title,

        @NotNull(message = "authorId is required")
        @Positive(message = "authorId must be a positive number")
        Long authorId,

        Set<Long> genreIds,

        @NotNull(message = "price is required")
        @Positive(message = "price must be greater than 0")
        @Digits(integer = 8, fraction = 2, message = "price must have at most 2 decimal places")
        BigDecimal price) {
}

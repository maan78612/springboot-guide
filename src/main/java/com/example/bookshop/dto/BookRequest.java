package com.example.bookshop.dto;

import java.math.BigDecimal;
import java.util.Set;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Validation rules and allowed fields for BookRequest.
 *
 * | Key / Constraint | Field    | Explanation                                         |
 * |------------------|----------|-----------------------------------------------------|
 * | @NotBlank        | title    | Rejects null, empty "", or whitespace-only values   |
 * | @Size(max = 200) | title    | Limits the title to at most 200 characters          |
 * | @NotNull         | authorId | References an existing author id                    |
 * | @Positive        | authorId | Rejects zero or negative author ids                 |
 * | genreIds         | genreIds | Optional set of genre foreign key IDs               |
 * | @NotNull         | price    | Rejects a missing or null price                     |
 * | @Positive        | price    | Price must be greater than 0                        |
 * | @Digits(8, 2)    | price    | Up to 8 digits before and 2 after the decimal point |
 */
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

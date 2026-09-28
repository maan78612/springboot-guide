/*
 ^ TUTORIAL 12 — inbound shape for the bulk discount endpoint.
*/
package com.example.bookshop.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Validated request body for an author's bulk discount.
 *
 * | Key / Constraint | Why we use it                                  |
 * |------------------|------------------------------------------------|
 * | @NotNull          | Requires the client to provide a percentage    |
 * | @Min(1)           | Rejects zero and negative discounts             |
 * | @Max(90)          | Prevents discounts above the allowed maximum    |
 */
public record DiscountRequest(

		@NotNull(message = "percent is required") @Min(value = 1, message = "percent must be between 1 and 90") @Max(value = 90, message = "percent must be between 1 and 90") Integer percent) {
}

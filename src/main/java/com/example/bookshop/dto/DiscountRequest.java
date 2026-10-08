package com.example.bookshop.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Inbound request body for the bulk author discount endpoint.
 *
 * | Key / Constraint | Field   | Explanation                                  |
 * |------------------|---------|----------------------------------------------|
 * | @NotNull         | percent | Requires a discount percentage to be provided|
 * | @Min(1)          | percent | Minimum discount percentage is 1%            |
 * | @Max(90)         | percent | Maximum discount percentage is 90%           |
 */
public record DiscountRequest(
        @NotNull(message = "percent is required")
        @Min(value = 1, message = "percent must be between 1 and 90")
        @Max(value = 90, message = "percent must be between 1 and 90")
        Integer percent) {
}


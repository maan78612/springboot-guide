/*
 ^ TUTORIAL 07 — what the client is ALLOWED to send
 + No id (database assigns), no costPrice (internal). Unknown fields
 +   bind to nothing and vanish - mass-assignment safe.

 ^ TUTORIAL 08 — constraints + messages; armed by @Valid in the
 ?   controller. Without @Valid they are decoration.

 ^ TUTORIAL 10 — relationships arrive by ID

 ? The client does not send an author OBJECT - it references one:
 ?   "authorId": 2. Same for genres: "genreIds": [1, 3].
 ? The service resolves ids to entities and rejects unknown ones.
 * genreIds may be omitted entirely - not every book is categorized.
*/
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

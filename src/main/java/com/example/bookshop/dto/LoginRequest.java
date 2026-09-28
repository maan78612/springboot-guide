/*
 ^ TUTORIAL 15 — login input.
*/
package com.example.bookshop.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Validated credentials accepted by the login endpoint.
 *
 * | Key / Constraint | Why we use it                                  |
 * |------------------|------------------------------------------------|
 * | @NotBlank        | Requires both email and password               |
 * | @Email           | Rejects values that are not email-shaped        |
 */
public record LoginRequest(

		@NotBlank(message = "email is required") @Email(message = "email must be a valid email address") String email,

		@NotBlank(message = "password is required") String password) {
}

/*
 ^ TUTORIAL 15 — register input

 ? @Size(min = 8): the ONE password rule worth enforcing is length.
 ? max = 72 because BCrypt only reads the first 72 bytes - accepting
 ? more would silently ignore the rest of the password.
 * Note there is no "role" field: you cannot register yourself as
 * an admin. What is absent from a DTO is a security decision.
*/
package com.example.bookshop.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

		@NotBlank(message = "name is required")
		@Size(max = 100, message = "name must be at most 100 characters")
		String name,

		@NotBlank(message = "email is required")
		@Email(message = "email must be a valid email address")
		String email,

		@NotBlank(message = "password is required")
		@Size(min = 8, max = 72, message = "password must be 8-72 characters")
		String password) {
}

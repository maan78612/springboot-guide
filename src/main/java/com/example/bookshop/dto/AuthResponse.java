/*
 ^ TUTORIAL 15 — what login returns

 ? tokenType "Bearer" spells out how to use the token:
 ?   Authorization: Bearer <token>
*/
package com.example.bookshop.dto;

/**
 * Login response containing the bearer token and safe account details.
 *
 * | Key              | Why we use it                                     |
 * |------------------|---------------------------------------------------|
 * | token            | Carries the signed credential for later requests  |
 * | tokenType        | Tells clients to send the token as Bearer          |
 * | expiresInMinutes | Tells clients the token lifetime                  |
 * | user             | Returns account details without the password hash |
 */
public record AuthResponse(String token, String tokenType, long expiresInMinutes,
		UserResponse user) {

	public static AuthResponse bearer(String token, long expiresInMinutes, UserResponse user) {
		return new AuthResponse(token, "Bearer", expiresInMinutes, user);
	}
}

/*
 ^ TUTORIAL 15 — what login returns

 ? tokenType "Bearer" spells out how to use the token:
 ?   Authorization: Bearer <token>
*/
package com.example.bookshop.dto;

public record AuthResponse(String token, String tokenType, long expiresInMinutes,
		UserResponse user) {

	public static AuthResponse bearer(String token, long expiresInMinutes, UserResponse user) {
		return new AuthResponse(token, "Bearer", expiresInMinutes, user);
	}
}

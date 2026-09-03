/*
 ^ TUTORIAL 15 — outbound account shape

 + No passwordHash component = the hash CANNOT leak, same argument
 +   as costPrice in tutorial 07. The DTO layer earning its keep on
 +   the most sensitive column we have.
*/
package com.example.bookshop.dto;

import com.example.bookshop.model.UserAccount;

public record UserResponse(Long id, String name, String email, String role) {

	public static UserResponse from(UserAccount user) {
		return new UserResponse(user.getId(), user.getName(), user.getEmail(),
				user.getRole().name());
	}
}

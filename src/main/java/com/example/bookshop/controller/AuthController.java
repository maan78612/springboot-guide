/*
 ^ TUTORIAL 15 — the auth endpoints

 ? POST /api/v1/auth/register  open   -> 201 + account (no token)
 ? POST /api/v1/auth/login     open   -> 200 + JWT + account
 ? GET  /api/v1/auth/me        Bearer -> 200 + who the token says

 ? @AuthenticationPrincipal Jwt jwt hands the VALIDATED token to the
 ? method - by the time we run, the signature and expiry already
 ? checked out (the resource-server filter rejected bad ones with
 ? 401 before any controller). jwt.getSubject() is the email we put
 ? in at login.
*/
package com.example.bookshop.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bookshop.dto.ApiResponse;
import com.example.bookshop.dto.AuthResponse;
import com.example.bookshop.dto.LoginRequest;
import com.example.bookshop.dto.RegisterRequest;
import com.example.bookshop.dto.UserResponse;
import com.example.bookshop.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	public ResponseEntity<ApiResponse<UserResponse>> register(
			@Valid @RequestBody RegisterRequest request) {
		UserResponse user = UserResponse.from(authService.register(request));
		return ResponseEntity.status(201)
				.body(ApiResponse.ok("Account created", user));
	}

	@PostMapping("/login")
	public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.ok("Logged in", authService.login(request));
	}

	@GetMapping("/me")
	public ApiResponse<UserResponse> me(@AuthenticationPrincipal Jwt jwt) {
		return ApiResponse.ok("Current account",
				UserResponse.from(authService.getByEmail(jwt.getSubject())));
	}
}

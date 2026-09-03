/*
 ^ TUTORIAL 09 — the one exception our own code throws at clients

 ? An "operational" error: something we EXPECT to happen in normal
 ? use (unknown id, duplicate ISBN, no permission) and are happy to
 ? explain to the client. The opposite - a programmer bug - must NOT
 ? be explained to clients; the global handler hides those as a
 ? generic 500.

 ? It extends RuntimeException (unchecked) on purpose: services can
 ? throw it from anywhere without polluting every signature with
 ? "throws", and it flies up to the global handler on its own.

 + Usage anywhere in a service:
 +   throw ApiException.notFound("Book with id 42 not found");
 * The static factories mirror ApiError in the node reference - one
 * per HTTP status we actually use.
*/
package com.example.bookshop.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

	private final HttpStatus status;

	public ApiException(HttpStatus status, String message) {
		super(message);
		this.status = status;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public static ApiException badRequest(String message) {
		return new ApiException(HttpStatus.BAD_REQUEST, message);
	}

	public static ApiException unauthorized(String message) {
		return new ApiException(HttpStatus.UNAUTHORIZED, message);
	}

	public static ApiException forbidden(String message) {
		return new ApiException(HttpStatus.FORBIDDEN, message);
	}

	public static ApiException notFound(String message) {
		return new ApiException(HttpStatus.NOT_FOUND, message);
	}

	public static ApiException conflict(String message) {
		return new ApiException(HttpStatus.CONFLICT, message);
	}
}

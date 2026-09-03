/*
 ^ TUTORIAL 09 — the failure twin of ApiResponse

 ? Every error leaving this API has ONE shape:
 ?   { "success": false, "message": "...", "errors": [ ... ] }
 ? "errors" carries field-level details for validation failures and
 ? is omitted (NON_NULL) everywhere else.
 * Clients branch on "success" and can always show "message" - same
 * contract as the node-mongo-helper reference.
*/
package com.example.bookshop.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(boolean success, String message, List<FieldViolation> errors) {

	public record FieldViolation(String field, String message) {
	}

	public static ErrorResponse of(String message) {
		return new ErrorResponse(false, message, null);
	}

	public static ErrorResponse of(String message, List<FieldViolation> errors) {
		return new ErrorResponse(false, message, errors);
	}
}

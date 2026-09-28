/*
 ^ TUTORIAL 07 — one envelope for every response

 ? Every endpoint in this API answers with the same JSON shape:
 ?   { "success": true, "message": "...", "data": ..., "meta": ... }
 ? One shape means every consumer (frontend, mobile, another service)
 ? handles every endpoint with the same code path. Same envelope as
 ? the node-mongo-helper reference project, on purpose.

 ? This is a RECORD - Java's short form for an immutable data class:
 ?   record ApiResponse<T>(boolean success, ...) declares the fields,
 ?   constructor, accessors (success(), data()), equals/hashCode -
 ?   all generated. Perfect for DTOs, which are dumb data carriers.
 ? Jackson handles records natively: components become JSON fields
 ? (no getters needed), and the canonical constructor is used for
 ? reading JSON (no setters needed).

 ? @JsonInclude(NON_NULL): fields that are null stay OUT of the JSON.
 ? So "meta" only appears on responses that have it (paginated lists,
 ? tutorial 10) and "data" disappears on data-less messages.

 * The error twin ({"success": false, ...}) arrives in tutorial 09,
 * built by the global error handler - not here.
*/
package com.example.bookshop.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard success body returned by the API.
 *
 * | Key             | Why we use it                                      |
 * |-----------------|----------------------------------------------------|
 * | @JsonInclude    | Omits optional fields whose value is null          |
 * | success         | Lets clients identify a successful response        |
 * | message         | Gives a short human-readable result                 |
 * | T data          | Holds a response DTO or a collection of DTOs        |
 * | meta            | Holds optional paging or other response metadata    |
 * | ok(...) methods | Provide convenient factories with or without meta  |
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, String message, T data, Object meta) {

	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(true, "Success", data, null);
	}

	public static <T> ApiResponse<T> ok(String message, T data) {
		return new ApiResponse<>(true, message, data, null);
	}

	public static <T> ApiResponse<T> ok(String message, T data, Object meta) {
		return new ApiResponse<>(true, message, data, meta);
	}
}

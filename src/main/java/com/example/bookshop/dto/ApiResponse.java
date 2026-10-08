package com.example.bookshop.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Shared successful-response structure for API endpoints.
 *
 * | Key             | Why we use it                                      |
 * |-----------------|----------------------------------------------------|
 * | @JsonInclude    | Omits optional fields whose value is null          |
 * | T data          | Holds one response DTO or a list of DTOs            |
 * | meta            | Holds optional paging or other response metadata    |
 * | ok(...)         | Creates the common successful-response form         |
 */
/*
 * @JsonInclude(JsonInclude.Include.NON_NULL) tells Jackson, the JSON library
 * Spring uses, to skip fields whose value is null. For example, if meta is
 * null, the response omits it instead of returning "meta": null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
/*
 * This record defines the common response fields. The generic type T lets
 * data hold different kinds of results, such as one book or a list of books.
 */
public record ApiResponse<T>(boolean success, String message, T data, Object meta) {

    /*
     * These two methods use method overloading: Java has no optional
     * parameters, so this version is for responses that do not need meta.
     * It sets meta to null, so callers do not have to pass null themselves.
     */
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    // Use this version when the response includes extra information, such as pagination.
    public static <T> ApiResponse<T> ok(String message, T data, Object meta) {
        return new ApiResponse<>(true, message, data, meta);
    }
}

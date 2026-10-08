package com.example.bookshop.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Shared JSON body for API failures.
 *
 * | Key / Annotation | Why we use it                                     |
 * |------------------|---------------------------------------------------|
 * | @JsonInclude     | Omits errors when no field-level details exist    |
 * | success          | Lets clients identify the response as a failure  |
 * | message          | Gives a safe summary of the problem               |
 * | FieldViolation   | Identifies a request field and its validation text|
 */
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

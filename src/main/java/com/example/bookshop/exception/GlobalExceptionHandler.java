/*
 ^ TUTORIAL 09 — one place where every error becomes JSON

 ? @RestControllerAdvice = a bean whose @ExceptionHandler methods
 ? apply to ALL controllers. Any exception escaping a controller
 ? lands here; Spring picks the MOST SPECIFIC matching handler.
 ? This is the Spring twin of error.middleware.js in the node
 ? reference: controllers and services just throw; translation to
 ? HTTP happens in exactly one file.

 ? The split that matters:
 ?   expected problems  -> honest status + human message
 ?   unexpected BUGS    -> 500, generic message, full details only
 ?                         in OUR log. Clients never see stack
 ?                         traces or internal messages.

 ! An @ExceptionHandler method placed inside a controller class works
 ! too - but only for THAT controller. Global shapes belong here.
 ! Also: this only catches exceptions from the controller layer down.
 ! Errors thrown in security filters (tutorial 16) never reach it and
 ! need their own handling.
*/
package com.example.bookshop.exception;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.example.bookshop.dto.ErrorResponse;
import com.example.bookshop.dto.ErrorResponse.FieldViolation;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	// ── Our own thrown errors ───────────────────────────────────────
	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
		return ResponseEntity.status(ex.getStatus()).body(ErrorResponse.of(ex.getMessage()));
	}

	// ── @Valid failures: surface every field problem ────────────────
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		List<FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
				.toList();
		return ResponseEntity.badRequest().body(ErrorResponse.of("Validation failed", violations));
	}

	// ── Body is not valid JSON at all ───────────────────────────────
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
		return ResponseEntity.badRequest().body(ErrorResponse.of("Malformed JSON in request body"));
	}

	// ── /books/abc where a number was expected ──────────────────────
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		String message = "Parameter '" + ex.getName() + "' has an invalid value: '" + ex.getValue() + "'";
		return ResponseEntity.badRequest().body(ErrorResponse.of(message));
	}

	// ── URL that matches nothing ────────────────────────────────────
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ErrorResponse.of("No endpoint " + ex.getHttpMethod() + " /" + ex.getResourcePath()));
	}

	// ── Right URL, wrong verb ───────────────────────────────────────
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
				.body(ErrorResponse.of(ex.getMethod() + " is not allowed here"));
	}

	// ── Body sent without Content-Type: application/json ────────────
	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException ex) {
		return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
				.body(ErrorResponse.of("Content-Type must be application/json"));
	}

	/*
	 ^ TUTORIAL 16 — @PreAuthorize denials land HERE, not in the filter
	 ? URL rules fail in the FILTER chain (-> ApiAuthErrorHandler);
	 ? method rules fail INSIDE the controller call, as an
	 ? AccessDeniedException - which the catch-all below would turn
	 ? into a lying 500. Found by testing: a seller calling an
	 ? admin-only endpoint got 500 until this handler existed.
	 */
	@ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> handleAccessDenied(
			org.springframework.security.access.AccessDeniedException ex) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(ErrorResponse.of("You do not have permission to perform this action"));
	}

	// ── Everything else is a BUG: hide details, log everything ──────
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
		log.error("Unhandled exception", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ErrorResponse.of("Something went wrong. Please try again later."));
	}
}

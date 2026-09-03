/*
 ^ TUTORIAL 15 — 401/403 in our envelope, even from the filter layer

 ? Security failures happen in FILTERS, before any controller exists,
 ? so @RestControllerAdvice never sees them (tutorial 09 warned about
 ? exactly this). This class is the fix - Spring Security calls it:
 ?   AuthenticationEntryPoint.commence -> not logged in       -> 401
 ?   AccessDeniedHandler.handle        -> logged in, no right -> 403
 ? Both write the same ErrorResponse JSON the rest of the API uses.

 ? 401 vs 403, once and forever:
 ?   401 Unauthorized = "I don't know WHO you are" (no/bad token)
 ?   403 Forbidden    = "I know who you are. No."  (role too small)
*/
package com.example.bookshop.config;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.example.bookshop.dto.ErrorResponse;

// Jackson 3 (Spring Boot 4) lives in tools.jackson, not
// com.fasterxml.jackson - only the ANNOTATIONS kept the old package.
import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiAuthErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

	private final ObjectMapper objectMapper;

	public ApiAuthErrorHandler(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		write(response, HttpServletResponse.SC_UNAUTHORIZED,
				"Authentication required: send a valid Bearer token");
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		write(response, HttpServletResponse.SC_FORBIDDEN,
				"You do not have permission to perform this action");
	}

	private void write(HttpServletResponse response, int status, String message)
			throws IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getWriter(), ErrorResponse.of(message));
	}
}

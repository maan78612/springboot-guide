/*
 ^ TUTORIAL 16 — rate limiting the auth endpoints

 ? Login is the endpoint attackers hammer: try passwords until one
 ? works. BCrypt makes each try slow; a rate limit makes many tries
 ? impossible. This filter allows N auth requests per IP per minute
 ? (bookshop.security.auth-rate-limit-per-minute); the rest get 429.

 ? OncePerRequestFilter = runs exactly once per request, BEFORE the
 ? security chain and controllers. The implementation is a "fixed
 ? window": one counter per IP, reset every minute. ~30 lines, no
 ? dependencies, and honest about its limits:
 ! - counters live in THIS process: run two instances and each has
 !   its own budget. Real multi-instance setups count in a shared
 !   store (Redis) or at the gateway. Same caveat as the in-memory
 !   express-rate-limit default in the node reference.
 ! - behind a proxy/load balancer, getRemoteAddr() is the PROXY's
 !   address; production reads the X-Forwarded-For header instead
 !   (and must only trust it FROM the proxy).
*/
package com.example.bookshop.config;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.bookshop.dto.ErrorResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/*
 ! Deliberately NOT @Component: SecurityConfig constructs it and adds
 ! it to the chain itself. As a scanned bean it would ALSO be
 ! registered as a plain servlet filter (running twice), and the
 ! @WebMvcTest slice would try to build it without its dependencies
 ! (that exact failure is in the tutorial doc).
 */
public class RateLimitFilter extends OncePerRequestFilter {

	private record Window(long startedAtMinute, AtomicInteger count) {
	}

	private final Map<String, Window> windows = new ConcurrentHashMap<>();
	private final BookshopProperties properties;
	private final ObjectMapper objectMapper;

	public RateLimitFilter(BookshopProperties properties, ObjectMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !request.getRequestURI().startsWith("/api/v1/auth/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		long nowMinute = System.currentTimeMillis() / 60_000;
		Window window = windows.compute(request.getRemoteAddr(), (ip, current) ->
				(current == null || current.startedAtMinute() != nowMinute)
						? new Window(nowMinute, new AtomicInteger())
						: current);
		if (window.count().incrementAndGet() > properties.getSecurity().getAuthRateLimitPerMinute()) {
			response.setStatus(429);
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			objectMapper.writeValue(response.getWriter(),
					ErrorResponse.of("Too many attempts. Try again in a minute."));
			return;
		}
		filterChain.doFilter(request, response);
	}
}

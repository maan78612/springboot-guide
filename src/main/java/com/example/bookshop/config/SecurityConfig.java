/*
 ^ TUTORIAL 15 — the security rules of this API, in one place

 ? @Configuration marks a class whose @Bean methods hand objects to
 ? the container - the third way to make beans (after @Component
 ? scanning and @ConfigurationProperties): for objects that need
 ? assembly logic, not just a constructor.

 ? The big picture: Spring Security is a chain of FILTERS that run
 ? BEFORE any controller. Our SecurityFilterChain bean replaces the
 ? lock-everything default with our rules:
 ?
 ?   +--------------------------------------+----------------------+
 ?   | Request                              | Rule                 |
 ?   +--------------------------------------+----------------------+
 ?   | POST /api/v1/auth/register|login     | open to everyone     |
 ?   | GET  /api/v1/books/deleted           | logged-in (tut. 16   |
 ?   |                                      | tightens to admin)   |
 ?   | GET  books / authors / shop          | open (public catalog)|
 ?   | /h2-console/**                       | open, DEV database UI|
 ?   | EVERYTHING ELSE                      | needs a valid JWT    |
 ?   +--------------------------------------+----------------------+
 ? Matchers run IN ORDER; first hit wins - the specific
 ? /books/deleted line must sit above the /books/** line.
 + anyRequest().authenticated() last = allow-list thinking: new
 +   endpoints are BORN protected; you opt them INTO being public.

 ? Stateless API decisions:
 ?   csrf.disable  - CSRF protection defends session cookies; we
 ?                   have no sessions, browsers never auto-send a
 ?                   JWT. (Never disable it on a cookie/session app!)
 ?   STATELESS     - no HTTP session; every request proves itself
 ?                   with its token.

 ? JWT wiring: HS256 = "sign with a shared secret key".
 ?   JwtEncoder (login creates tokens) and JwtDecoder (the resource
 ?   server validates Authorization: Bearer headers) are built from
 ?   the SAME configured secret.
 ? The converter turns the token's "role" claim into ROLE_USER /
 ?   ROLE_ADMIN authorities - tutorial 16 builds on those.

 ! Errors thrown by these filters never reach GlobalExceptionHandler
 !   (tutorial 09's warning). ApiAuthErrorHandler is wired here so
 !   401/403 still answer in OUR error envelope.
*/
package com.example.bookshop.config;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	private static final String HMAC = "HmacSHA256";

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http,
			JwtAuthenticationConverter jwtAuthenticationConverter,
			ApiAuthErrorHandler errorHandler, BookshopProperties properties,
			tools.jackson.databind.ObjectMapper objectMapper) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				// TUTORIAL 16: browser cross-origin rules (bean below)
				.cors(Customizer.withDefaults())
				// TUTORIAL 16: auth endpoints are rate limited per IP
				.addFilterBefore(new RateLimitFilter(properties, objectMapper),
						UsernamePasswordAuthenticationFilter.class)
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/v1/auth/register", "/api/v1/auth/login").permitAll()
						// TUTORIAL 16: was authenticated(); now admin-only at
						// the URL level too (method @PreAuthorize = 2nd lock)
						.requestMatchers(HttpMethod.GET, "/api/v1/books/deleted").hasRole("ADMIN")
						.requestMatchers(HttpMethod.GET,
								"/api/v1/books/**", "/api/v1/authors/**", "/api/v1/shop").permitAll()
						.requestMatchers("/h2-console/**").permitAll()
						// TUTORIAL 17: API docs. Born protected (verified: 401
						// until these lines) - opened deliberately. Prod turns
						// springdoc off entirely instead.
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
						.permitAll()
						// TUTORIAL 19: load balancers and orchestrators probe
						// this anonymously. Only health - the other actuator
						// endpoints stay behind anyRequest().authenticated().
						.requestMatchers("/actuator/health").permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth -> oauth
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
						.authenticationEntryPoint(errorHandler))
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint(errorHandler)
						.accessDeniedHandler(errorHandler))
				// the H2 console renders itself in a frame; allow same-origin
				.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
		return http.build();
	}

	/*
	 ^ TUTORIAL 16 — CORS
	 ? Browsers block a page on origin A from reading API responses on
	 ? origin B unless B's CORS headers allow it. This bean answers
	 ? preflight (OPTIONS) requests for the origins in configuration.
	 ! CORS protects BROWSER users; curl and servers ignore it. It is
	 !   not authentication - it decides which WEBSITES may embed
	 !   calls to us, nothing more.
	 */
	@Bean
	CorsConfigurationSource corsConfigurationSource(BookshopProperties properties) {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(properties.getSecurity().getCorsAllowedOrigins());
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", config);
		return source;
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	JwtEncoder jwtEncoder(BookshopProperties properties) {
		SecretKeySpec key = new SecretKeySpec(
				properties.getSecurity().getJwtSecret().getBytes(), HMAC);
		return new NimbusJwtEncoder(new ImmutableSecret<>(key));
	}

	@Bean
	JwtDecoder jwtDecoder(BookshopProperties properties) {
		SecretKeySpec key = new SecretKeySpec(
				properties.getSecurity().getJwtSecret().getBytes(), HMAC);
		return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
	}

	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(jwt -> {
			String role = jwt.getClaimAsString("role");
			return role == null ? List.of()
					: List.of(new SimpleGrantedAuthority("ROLE_" + role));
		});
		return converter;
	}
}

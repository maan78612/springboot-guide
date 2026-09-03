/*
 ^ TUTORIAL 15 — register, login, and minting tokens

 ? register: hash the password with BCrypt, store the HASH, forget
 ?   the password. Role is always USER - admins are made by seeding
 ?   or by other admins, never by self-registration.

 ? login: find the account, let BCrypt compare the guess against the
 ?   stored hash (encoder.matches). On success, mint a JWT.
 + Unknown email and wrong password answer with the SAME message.
 +   "Wrong password" would confirm the email exists - a free gift
 +   to attackers ("user enumeration").

 ? A JWT is three base64 parts: header.payload.signature.
 ?   payload = claims (who: sub, until when: exp, what: role).
 ?   signature = HMAC of the rest with our secret key. Anyone can
 ?   READ a JWT (it is not encrypted!) - nobody can FORGE one
 ?   without the key. Never put secrets in claims.
 ? Stateless: the server stores nothing per login. Every request
 ?   proves itself; logout = the client deletes its copy; a stolen
 ?   token works until exp - which is why the TTL is short.
*/
package com.example.bookshop.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.example.bookshop.config.BookshopProperties;
import com.example.bookshop.dto.AuthResponse;
import com.example.bookshop.dto.LoginRequest;
import com.example.bookshop.dto.RegisterRequest;
import com.example.bookshop.dto.UserResponse;
import com.example.bookshop.exception.ApiException;
import com.example.bookshop.model.Role;
import com.example.bookshop.model.UserAccount;
import com.example.bookshop.repository.UserAccountRepository;

@Service
public class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	private final UserAccountRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtEncoder jwtEncoder;
	private final BookshopProperties properties;

	public AuthService(UserAccountRepository userRepository, PasswordEncoder passwordEncoder,
			JwtEncoder jwtEncoder, BookshopProperties properties) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtEncoder = jwtEncoder;
		this.properties = properties;
	}

	public UserAccount register(RegisterRequest request) {
		if (userRepository.existsByEmailIgnoreCase(request.email())) {
			throw ApiException.conflict("An account with this email already exists");
		}
		UserAccount user = new UserAccount(
				request.name(),
				request.email().toLowerCase(),
				passwordEncoder.encode(request.password()),
				Role.USER);
		UserAccount saved = userRepository.save(user);
		log.info("Account registered: id={}", saved.getId());
		return saved;
	}

	public AuthResponse login(LoginRequest request) {
		UserAccount user = userRepository.findByEmailIgnoreCase(request.email())
				.orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw ApiException.unauthorized("Invalid email or password");
		}
		long ttlMinutes = properties.getSecurity().getTokenTtlMinutes();
		String token = mintToken(user, ttlMinutes);
		log.info("Login: userId={}", user.getId());
		return AuthResponse.bearer(token, ttlMinutes, UserResponse.from(user));
	}

	public UserAccount getByEmail(String email) {
		return userRepository.findByEmailIgnoreCase(email)
				.orElseThrow(() -> ApiException.unauthorized("Account no longer exists"));
	}

	private String mintToken(UserAccount user, long ttlMinutes) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("bookshop")
				.subject(user.getEmail())
				.claim("userId", user.getId())
				.claim("role", user.getRole().name())
				.issuedAt(now)
				.expiresAt(now.plus(ttlMinutes, ChronoUnit.MINUTES))
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}
}

/*
 ^ TUTORIAL 04 — typed configuration with @ConfigurationProperties

 ? This class IS the "bookshop.*" section of application.properties,
 ? as a Java object. Spring reads the file at startup and fills the
 ? fields through the setters ("binding"):
 ?   bookshop.shop-name                 -> setShopName(...)
 ?   bookshop.catalog.default-page-size -> getCatalog().setDefaultPageSize(...)
 ? kebab-case in the file maps to camelCase here automatically.

 ? Why this beats sprinkling @Value everywhere:
 ?   one class shows every setting that exists, with its type;
 ?   a typo fails ONCE here, not in whichever class read it;
 ?   validation below rejects bad values AT STARTUP.

 ? @Validated + the constraints make the app fail fast: it refuses
 ? to boot with a blank currency or max-page-size=0, instead of
 ? failing at 2 a.m. on the first request that uses the value.
 ? (Same idea as validating process.env at boot in Node.)

 * Registered by @ConfigurationPropertiesScan on BookshopApplication.
 * More constraint annotations in tutorial 08.
*/
package com.example.bookshop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@ConfigurationProperties(prefix = "bookshop")
@Validated
public class BookshopProperties {

	@NotBlank
	private String shopName;

	@NotBlank
	private String currency;

	@Valid
	private final Catalog catalog = new Catalog();

	@Valid
	private final Security security = new Security();

	private final Admin admin = new Admin();

	public String getShopName() {
		return shopName;
	}

	public void setShopName(String shopName) {
		this.shopName = shopName;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public Catalog getCatalog() {
		return catalog;
	}

	public Security getSecurity() {
		return security;
	}

	public Admin getAdmin() {
		return admin;
	}

	/*
	 ^ TUTORIAL 15 — security settings
	 ? jwt-secret signs tokens (HS256 needs at least 32 characters -
	 ?   the @Size floor makes a too-short secret fail AT STARTUP).
	 ? The value in application.properties is a DEV value; the prod
	 ?   profile forces it from the JWT_SECRET environment variable.
	 */
	public static class Security {

		@NotBlank
		@jakarta.validation.constraints.Size(min = 32,
				message = "jwt-secret must be at least 32 characters")
		private String jwtSecret;

		@Min(5)
		private int tokenTtlMinutes = 60;

		// TUTORIAL 16: browsers from these origins may call the API.
		private java.util.List<String> corsAllowedOrigins =
				java.util.List.of("http://localhost:3000");

		// TUTORIAL 16: login/register attempts per IP per minute.
		@Min(1)
		private int authRateLimitPerMinute = 10;

		public String getJwtSecret() {
			return jwtSecret;
		}

		public void setJwtSecret(String jwtSecret) {
			this.jwtSecret = jwtSecret;
		}

		public int getTokenTtlMinutes() {
			return tokenTtlMinutes;
		}

		public void setTokenTtlMinutes(int tokenTtlMinutes) {
			this.tokenTtlMinutes = tokenTtlMinutes;
		}

		public java.util.List<String> getCorsAllowedOrigins() {
			return corsAllowedOrigins;
		}

		public void setCorsAllowedOrigins(java.util.List<String> corsAllowedOrigins) {
			this.corsAllowedOrigins = corsAllowedOrigins;
		}

		public int getAuthRateLimitPerMinute() {
			return authRateLimitPerMinute;
		}

		public void setAuthRateLimitPerMinute(int authRateLimitPerMinute) {
			this.authRateLimitPerMinute = authRateLimitPerMinute;
		}
	}

	/*
	 ? First-admin bootstrap (tutorial 15). Both values blank -> no
	 ? seeding. The password value never appears in any log.
	 */
	public static class Admin {

		private String email = "";
		private String password = "";

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}
	}

	public static class Catalog {

		@Min(1)
		private int defaultPageSize = 10;

		@Min(1)
		@Max(500)
		private int maxPageSize = 100;

		public int getDefaultPageSize() {
			return defaultPageSize;
		}

		public void setDefaultPageSize(int defaultPageSize) {
			this.defaultPageSize = defaultPageSize;
		}

		public int getMaxPageSize() {
			return maxPageSize;
		}

		public void setMaxPageSize(int maxPageSize) {
			this.maxPageSize = maxPageSize;
		}
	}
}

# Tutorial 15 — Auth

Add security and signed user tokens.

Files for this stage:

- Updated: `pom.xml`
- New: `src/main/java/com/example/bookshop/model/UserAccount.java`
- New: `src/main/java/com/example/bookshop/model/Role.java`
- New: `src/main/java/com/example/bookshop/repository/UserAccountRepository.java`
- New: `src/main/java/com/example/bookshop/config/SecurityConfig.java`
- New: `src/main/java/com/example/bookshop/service/AuthService.java`
- New: `src/main/java/com/example/bookshop/controller/AuthController.java`
- New: `src/main/java/com/example/bookshop/dto/RegisterRequest.java`
- New: `src/main/java/com/example/bookshop/dto/LoginRequest.java`
- New: `src/main/java/com/example/bookshop/dto/AuthResponse.java`

---

## 1. Secure the app by default

Spring Security locks things down unless you explicitly allow endpoints.

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/auth/**").permitAll()
            .anyRequest().authenticated());
    return http.build();
}
```

## 2. Hash passwords and issue JWTs

```java
String passwordHash = passwordEncoder.encode(request.password());
String token = jwtService.generateToken(user);
```

This keeps the raw password out of the database and gives each authenticated request a signed token.

## 3. Login flow

```json
POST /api/v1/auth/login
{"email":"user@example.com","password":"secret"}
```

Response includes a bearer token and user metadata.

Next: [**Tutorial 16 — Roles and hardening**](tutorial-16%20%28Roles%20and%20hardening%29.md)

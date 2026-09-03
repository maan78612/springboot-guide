# Tutorial 15 — Auth

Spring Security's defaults, password hashing with BCrypt,
register/login, JWT tokens, and seeding the first admin.

Files for this stage:
- `pom.xml` (+ security, oauth2-resource-server, spring-security-test)
- `model/UserAccount.java`, `model/Role.java`,
  `repository/UserAccountRepository.java` (new)
- `config/SecurityConfig.java`, `config/ApiAuthErrorHandler.java`,
  `config/AdminSeeder.java` (new), `config/BookshopProperties.java`
  (+ security/admin sections)
- `dto/RegisterRequest|LoginRequest|AuthResponse|UserResponse.java`
- `service/AuthService.java`, `controller/AuthController.java`
- properties: dev admin credentials; prod requires env vars

---

## 1. Authentication vs authorization, then the defaults

```
+----------------+-------------------------------+----------------+
| Authentication | WHO are you?                  | this tutorial  |
| Authorization  | are YOU allowed to do THIS?   | tutorial 16    |
+----------------+-------------------------------+----------------+
```

Add one dependency — `spring-boot-starter-security` — change no
code, restart. Verified:

```
GET /api/v1/books  ->  401        (yesterday: 200)
log: Using generated security password: 88749e0d-bc48-432c-95c7-...
curl -u user:88749e0d-...  ->  200
```

Everything locked, a one-off password printed, and HTTP **basic
auth** (`-u user:password` = the credentials base64'd into a header,
every request) opens it. That is Spring Security's philosophy in one
demo: *secure by default, opt into openness* — the opposite of
Express, where your Node repo had to ADD auth middleware route by
route.

Why not stay with basic auth? The client must hold and send the raw
password forever, the server must hash-check it on EVERY request
(BCrypt is deliberately slow), and you cannot put claims (role, id)
in it. Its replacement:

## 2. JWT in five sentences

A **JWT** (JSON Web Token) is three base64 parts:
`header.payload.signature`. The payload holds **claims** — who you
are (`sub`), until when (`exp`), what you may do (`role`). The
signature is an HMAC of the rest with a server-side secret: anyone
can READ a JWT, nobody can FORGE one without the key. The server
stores nothing per login — each request carries its own proof
(`Authorization: Bearer <token>`). Login checks the password ONCE
and issues a token; after that, only fast signature checks.

Verified — the payload really is readable by anyone (base64, not
encryption!):

```
$ decode(token.split('.')[1])
{"iss":"bookshop","sub":"seller@example.com","role":"USER",
 "exp":1788433826,"iat":1788430226,"userId":2}
```

Never put anything secret in claims. And a stolen token works until
`exp` — which is why the TTL is 60 minutes, not 60 days.

## 3. Passwords: hash, never store

`UserAccount` (not `User` — a reserved word in Postgres) stores a
`passwordHash`, produced by **BCrypt**:

- one-way: a hash can verify a guess, never reveal the password;
- salted: two users with the same password get different hashes;
- deliberately slow: brute force pays full price per guess.

```java
passwordEncoder.encode(request.password())              // register
passwordEncoder.matches(guess, user.getPasswordHash())  // login
```

There is no decrypt anywhere. If your design can show anyone a
password, it is wrong. Two more deliberate details, both verified:

```
login with wrong password  -> 401 {"message":"Invalid email or password"}
login with unknown email   -> 401 {"message":"Invalid email or password"}
```

Identical answers — a different message for "no such user" would let
attackers harvest which emails have accounts (user enumeration).
And `RegisterRequest` has NO role field: what is absent from a DTO
is a security decision; you cannot register yourself as admin.

## 4. Our rules: the SecurityFilterChain

Security runs as FILTERS before any controller. `SecurityConfig`
replaces the default chain (see the file's header for the full
table): auth endpoints and catalog READS are public, everything else
needs a valid token; matchers are evaluated in order;
`anyRequest().authenticated()` closes the list so **new endpoints
are born protected** — allow-list thinking.

Two stateless-API decisions worth understanding:

- `csrf.disable()` — CSRF attacks exploit cookies that browsers
  attach automatically; we have no sessions and no cookies, nothing
  to exploit. (On a session/cookie app, disabling it is a hole.)
- `SessionCreationPolicy.STATELESS` — no server-side session ever.

And one lesson from tutorial 09 coming home: exceptions in filters
never reach `@RestControllerAdvice`, so `ApiAuthErrorHandler` is
wired into the chain to keep 401/403 in our envelope. 401 = "I don't
know who you are"; 403 = "I know you, and no" (tutorial 16 makes
403s happen).

## 5. The verified flow

```
POST /api/v1/auth/register {"name":"Test Seller","email":"seller@example.com",
                            "password":"seller-pass-123"}
201 {"success":true,"data":{"id":2,...,"role":"USER"}}
  (again) -> 409 "An account with this email already exists"

POST /api/v1/auth/login    -> 200
{"data":{"token":"eyJhbGciOiJIUzI1NiJ9...","tokenType":"Bearer",
         "expiresInMinutes":60,"user":{...}}}

GET  /api/v1/auth/me           (with Authorization: Bearer <token>)   -> 200
POST /api/v1/books             (with token)                           -> 201
POST /api/v1/books             (no token)  -> 401 envelope
GET  /api/v1/auth/me           (token + one character) -> 401
log: Seeded first admin account: admin@bookshop.local
```

The tamper test matters: change ONE character and the signature
check fails. That is the whole trust model.

How validation happens on the way in: the `oauth2-resource-server`
starter checks `Authorization: Bearer` headers with a `JwtDecoder`
built from the same secret the login's `JwtEncoder` signs with
(HS256 — one shared key does both). By the time a controller sees
`@AuthenticationPrincipal Jwt jwt`, signature and expiry already
passed.

## 6. Secrets and the first admin

The signing key lives in configuration, three layers deep:

```
application.properties       dev-grade key, committed ON PURPOSE -
                             it protects nothing and makes ./mvnw
                             spring-boot:run work out of the box
application-prod.properties  bookshop.security.jwt-secret=${JWT_SECRET}
                             no fallback: prod REFUSES to start
                             without the env var (fail fast, tut. 04)
validation                   @Size(min = 32): HS256 needs a real key;
                             a short one fails at startup
```

First admin (the chicken-and-egg: admins create admins, so who
creates admin #1?): `AdminSeeder`, a `CommandLineRunner` — a bean
Spring runs once after startup — creates the account from
`bookshop.admin.*` config if it does not exist. Dev has obviously
fake credentials in `application-dev.properties`; prod takes env
vars; blank means skip. The log line names the email, never the
password.

## 7. Tests, and what the slice does NOT test

The suite still passes — and one pass is suspicious: the slice test
posting `{}` without a token still got 400 (validation), not 401.
Verified conclusion: **@WebMvcTest does not enforce your security
chain** — it tests controllers, not the lock. So the lock's
regression test lives at the full level, permanently:

```java
@SpringBootTest ... mockMvc.perform(post("/api/v1/books")...content("{}"))
        .andExpect(status().isUnauthorized());
```

Suite: 11 tests, 0 failures.

## 8. The common mistakes

1. **Testing security in the wrong layer** — see above; the slice
   quietly skips your chain.
2. **`@Enumerated` without STRING** — roles stored as 0/1; reorder
   the enum someday and every USER becomes ADMIN. Always
   `EnumType.STRING`.
3. **Different login errors for unknown email vs wrong password** —
   free user enumeration. One message for both.
4. **Long-lived tokens** ("expires in 30 days, it's convenient") —
   a stolen token IS the account until it expires. Short TTL now;
   refresh tokens are the real-world extension (out of scope here).

## 9. Recap

- Security defaults: everything locked; you open doors explicitly,
  and unknown doors stay shut.
- BCrypt hash in the entity, never the password; enumeration-safe
  login; no role field on register.
- JWT: readable claims + unforgeable signature + stateless requests;
  encoder and decoder share one configured secret with a dev default
  and a prod env-var requirement.
- Filters fail outside the advice — the entry point/denied handler
  keep the envelope consistent.
- First admin comes from config via a seeder, idempotently.

Next: **Tutorial 16 — Roles and hardening**: admin-only endpoints,
sellers owning their books, CORS, rate limiting, and security
headers.

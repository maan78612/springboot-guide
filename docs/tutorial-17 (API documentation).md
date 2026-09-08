# Tutorial 17 — API documentation

OpenAPI, Swagger UI, and importing the spec into Postman.

Files for this stage:
- `pom.xml` (+ springdoc-openapi-starter-webmvc-ui, version pinned)
- `config/OpenApiConfig.java` (new)
- `config/SecurityConfig.java` (docs URLs permitted)
- `application-prod.properties` (docs OFF in prod)

---

## 1. Why: documentation that cannot lie

Hand-written API docs rot: someone renames a field, the wiki page
doesn't hear about it. The fix is docs GENERATED from the code:

```
+-------------+-----------------------------------------------------+
| OpenAPI     | a standard JSON description of an HTTP API: every   |
|             | path, parameter, shape, status code                 |
| springdoc   | the library that builds that JSON at runtime by     |
|             | reading our controllers, DTOs and validation rules  |
| Swagger UI  | a browsable web page rendered FROM the spec, with a |
|             | "Try it out" button per endpoint                    |
+-------------+-----------------------------------------------------+
```

One dependency does all three. Note the pom detail: springdoc is
third-party (`org.springdoc`), so unlike every Spring dependency it
needs an explicit `<version>` — the Boot parent only manages
Spring's own libraries.

## 2. The stumble first (on purpose)

Dependency added, restart, and:

```
GET /swagger-ui/index.html -> 401
GET /v3/api-docs           -> 401
{"success":false,"message":"Authentication required: send a valid Bearer token"}
```

This is THE classic springdoc-plus-security surprise — and it is
tutorial 16 working exactly as designed: `anyRequest().
authenticated()` means new endpoints are born protected, including
ones a library adds. The doors get opened deliberately in
SecurityConfig:

```java
.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
.permitAll()
```

When Swagger UI gives you 401s or an empty page, check the security
config before anything else.

## 3. What got generated (verified)

```
GET /v3/api-docs
openapi: 3.1.0 | title: Bookshop API
documented paths: 10
  /api/v1/auth/login    [post]         /api/v1/books          [get, post]
  /api/v1/auth/me       [get]          /api/v1/books/deleted  [get]
  /api/v1/auth/register [post]         /api/v1/books/{id}     [delete, get, put]
  /api/v1/authors       [get]          /api/v1/books/{id}/restore [post]
  /api/v1/authors/{id}/discount [post] ...
security scheme: [bearerAuth]

GET /swagger-ui/index.html -> 200
```

Every endpoint, parameter and DTO shape came from code we already
wrote — zero annotations needed. The one thing springdoc cannot
guess is in `OpenApiConfig`: the title, and the `bearerAuth`
security scheme. That scheme gives Swagger UI its **Authorize**
button: log in via `POST /api/v1/auth/login` (in the UI itself),
copy the token, click Authorize, paste — every "Try it out" now
sends the header. The whole API is clickable at

```
http://localhost:8080/swagger-ui/index.html
```

Want richer text on an endpoint? Optional annotations exist
(`@Operation(summary = ...)`, `@Tag`, `@ApiResponse`) — add them
where they earn their keep; the skeleton is free.

## 4. Postman

Your Node repo ships a hand-maintained Postman collection. Here it
is one import, always current:

Postman → Import → paste `http://localhost:8080/v3/api-docs` → done.
Every endpoint arrives as a request with typed parameters. Re-import
after API changes — the spec is regenerated on every restart, so it
is never stale. (Set the collection's auth to Bearer Token and paste
a login token once; requests inherit it.)

## 5. Docs in production

The prod profile turns springdoc off entirely:

```properties
springdoc.api-docs.enabled=false
springdoc.swagger-ui.enabled=false
```

A public API would expose docs on purpose. An internal one should
not hand strangers a map. Either is fine — decide, don't default.

## 6. Recap

- OpenAPI = machine-readable truth about the API; springdoc derives
  it from code; Swagger UI makes it clickable; Postman imports the
  same URL.
- Third-party deps pin their own versions in the pom.
- New URLs (even library-added ones) are born protected — open docs
  deliberately in dev, and decide explicitly for prod.

Next: [**Tutorial 18 — A real database**](tutorial-18%20%28A%20real%20database%29.md): Postgres and Flyway
migrations, and why create-drop + data.sql retire.

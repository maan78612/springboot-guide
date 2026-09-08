# Tutorial 13 — Testing

Unit tests with Mockito, the web slice with @WebMvcTest + MockMvc,
the whole app with @SpringBootTest — and what each level is actually
for.

Files for this stage (all under `src/test/java`):
- `service/BookServiceTest.java` (5 unit tests)
- `controller/BookControllerTest.java` (3 slice tests)
- `BookshopApplicationTests.java` (2 full tests)

Run everything:

```bash
./mvnw test
```

Real result:

```
Tests run: 5,  Failures: 0 -- in com.example.bookshop.service.BookServiceTest
Tests run: 3,  Failures: 0 -- in com.example.bookshop.controller.BookControllerTest
Tests run: 2,  Failures: 0 -- in com.example.bookshop.BookshopApplicationTests
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
```

---

## 1. Why three levels

One kind of test cannot check everything. Fast tests can't see the
whole; whole-app tests are too slow to run per keystroke. So tests
form a pyramid — many cheap ones at the bottom, few expensive ones
on top:

```
+------------------+-------------------+---------------------------------+
| Level            | Starts            | The RIGHT questions for it      |
+------------------+-------------------+---------------------------------+
| unit (Mockito)   | nothing - plain   | business rules: discount math,  |
| many, ~ms each   | Java objects      | duplicate rejection, clamping   |
+------------------+-------------------+---------------------------------+
| slice            | controller + JSON | URLs, status codes, JSON        |
| (@WebMvcTest)    | + validation +    | shapes, validation wiring,      |
| some             | error handler     | error envelopes                 |
+------------------+-------------------+---------------------------------+
| full             | EVERY bean + H2   | "does it all hang together":    |
| (@SpringBootTest)| + seed data       | a few end-to-end paths          |
| few              |                   |                                 |
+------------------+-------------------+---------------------------------+
```

## 2. Level 1 — unit tests: BookService with mocks

> A **mock** is a fake object that answers what you script and
> records what was called on it. Mockito builds them at runtime.

No Spring here at all — and THAT is tutorial 03's payoff. Because
`BookService` takes its dependencies through the constructor, a test
just constructs it with fakes:

```java
@ExtendWith(MockitoExtension.class)
class BookServiceTest {
    @Mock private BookRepository bookRepository;   // fakes
    @Mock private AuthorRepository authorRepository;
    @Mock private GenreRepository genreRepository;
    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookService = new BookService(bookRepository, authorRepository,
                genreRepository, new BookshopProperties());
    }
```

The five tests each show one tool:

```java
// script an answer
when(bookRepository.findById(42L)).thenReturn(Optional.empty());

// assert an exception, with details
assertThatThrownBy(() -> bookService.getBookById(42L))
        .isInstanceOf(ApiException.class)
        .hasMessage("Book with id 42 not found");

// assert something did NOT happen
verify(bookRepository, never()).save(any());

// catch what a mock was called with (proves the limit clamp works)
ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
verify(bookRepository).search(any(), any(), any(), any(), any(), pageable.capture());
assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
```

All five run in ~0.1 seconds combined. This level cannot see
`@Transactional` (proxies live in the container), SQL, or JSON —
wrong questions for it.

## 3. Level 2 — the web slice: @WebMvcTest

```java
@WebMvcTest(BookController.class)
class BookControllerTest {
    @Autowired  private MockMvc mockMvc;
    @MockitoBean private BookService bookService;
```

`@WebMvcTest` starts ONLY the web machinery for one controller —
JSON, validation, our GlobalExceptionHandler — no services, no
database. The service is a Mockito mock INSTALLED INTO the Spring
context by `@MockitoBean`.

! Boot 4 note: old tutorials say `@MockBean`. It was removed;
`@MockitoBean` (from spring-test) is the replacement. Same idea.

`MockMvc` fires requests without a network:

```java
mockMvc.perform(post("/api/v1/books")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.errors.length()").value(3));
```

That test proves the 400 CONTRACT — status, envelope, three field
errors — with zero real logic behind it. The 404 test proves the
advice translates a thrown `ApiException` even in the slice.

One wrinkle: entities made with `new` have `id = null` (ids come
from the database, which doesn't exist here), so the test sets them
via `ReflectionTestUtils.setField(book, "id", 1L)` — a test-only
escape hatch.

## 4. Level 3 — the whole app: @SpringBootTest

```java
@SpringBootTest
@AutoConfigureMockMvc
class BookshopApplicationTests {
```

Real context, real service, real repositories, real H2 with the
data.sql seeds; `@AutoConfigureMockMvc` bolts MockMvc on top. The
end-to-end test asserts seeded data comes through the entire stack
(`meta.total = 5`, first book "Effective Java").

And `contextLoads()` — the empty test from the generator — is not
useless: it fails if ANY bean cannot start. Cheapest smoke alarm
there is.

## 5. A real bug the tests caught (this actually happened)

The first full-suite run FAILED:

```
BookshopApplicationTests.fullStack_listBooks_servesSeededData:44
Status expected:<200> but was:<500>
Caused by: java.lang.AssertionError
    at org.hibernate.sql.ast.tree.from.CorrelatedTableGroup.addTableGroupJoin(...)
```

Every curl in tutorials 10–12 had passed — so why now? Surefire (the
Maven test runner) enables JVM assertions (`-ea`); the running app
does not. Hibernate has internal `assert` statements, and one of
them disliked how our search query's genre filter joined a
collection off a correlated root (`... in (select g.id from
b.genres g)`). Rewriting the filter as an EXISTS subquery over a
fresh root fixed it:

```java
and (:genreId is null or exists (
    select 1 from Book b2 join b2.genres g
    where b2.id = b.id and g.id = :genreId))
```

Suite green, live behavior identical (`?genreId=2` still returns
exactly "Effective Java"). Lesson: tests run the same code under
slightly different conditions — that difference is a feature. It
found a construction worth avoiding before some Hibernate upgrade
made it a production problem.

## 6. The common mistake — @WebMvcTest without the mock

Delete `@MockitoBean` and the slice test dies at startup:

```
Parameter 0 of constructor in com.example.bookshop.controller.BookController
required a bean of type 'com.example.bookshop.service.BookService' that
could not be found
```

Same message family as tutorial 03 — and the same meaning: the slice
does NOT create services; every dependency of the controller under
test must be provided (mocked) by you. When a slice test fails to
load its context, the missing `@MockitoBean` is suspect number one.

## 7. Recap

- Pyramid: many unit, some slice, few full — each level answers
  different questions.
- Constructor injection makes services unit-testable with plain
  `new` + mocks: when/thenReturn, verify, ArgumentCaptor.
- `@WebMvcTest` + `MockMvc` + `@MockitoBean` (not `@MockBean`!)
  test the HTTP contract without the backend.
- `@SpringBootTest` boots everything; keep it thin; `contextLoads`
  earns its place.
- `./mvnw test` runs with assertions enabled — sometimes stricter
  than production, and that is a gift.

Next: [**Tutorial 14 — Logging**](tutorial-14%20%28Logging%29.md): SLF4J, levels, dev-pretty vs
prod-JSON output, and what must never be logged.

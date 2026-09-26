# Tutorial 13 — Testing

Test at the right level for the right question.

Files for this stage:

- New: `src/test/java/com/example/bookshop/service/BookServiceTest.java`
- New: `src/test/java/com/example/bookshop/controller/BookControllerTest.java`
- New: `src/test/java/com/example/bookshop/BookshopApplicationTests.java`

---

## 1. Unit tests for business rules

```java
@ExtendWith(MockitoExtension.class)
class BookServiceTest {
    @Mock private BookRepository bookRepository;
    private BookService bookService;
}
```

These tests focus on logic such as validation, discount checks, and repository calls.

## 2. Web layer tests with MockMvc

```java
@WebMvcTest(BookController.class)
class BookControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private BookService bookService;
}
```

This checks HTTP status, JSON shape, and validation without booting the whole app.

## 3. Full app test with Spring Boot

```java
@SpringBootTest
@AutoConfigureMockMvc
class BookshopApplicationTests {
}
```

This validates wiring across real beans, repositories, and config.

Next: [**Tutorial 14 — Logging**](tutorial-14%20%28Logging%29.md)

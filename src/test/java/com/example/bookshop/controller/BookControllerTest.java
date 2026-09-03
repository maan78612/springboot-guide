/*
 ^ TUTORIAL 13 — level 2: the web slice (@WebMvcTest)

 ? @WebMvcTest(BookController.class) starts a SLICE of the app: this
 ? controller, the JSON machinery, validation, and our
 ? GlobalExceptionHandler - but NO services, NO repositories, NO
 ? database. The service is replaced with @MockitoBean: a Mockito
 ? mock placed INTO the Spring context.
 ? (Old tutorials show @MockBean - removed in Boot 4; the
 ? replacement is @MockitoBean from spring-test.)

 ? MockMvc performs requests without a real network socket:
 ?   mockMvc.perform(get(...)).andExpect(status().isOk())...
 ? jsonPath("$.data.title") digs into the response JSON.

 ? What this level is FOR: URLs, status codes, JSON shapes,
 ? validation wiring, error responses. Everything below is faked.

 ! ReflectionTestUtils sets the entity id - entities get ids from
 !   the database, which does not exist here. Test-only escape hatch.
*/
package com.example.bookshop.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.example.bookshop.exception.ApiException;
import com.example.bookshop.model.Author;
import com.example.bookshop.model.Book;
import com.example.bookshop.service.BookService;

@WebMvcTest(BookController.class)
class BookControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private BookService bookService;

	private Book sampleBook() {
		Author author = new Author("Joshua Bloch");
		ReflectionTestUtils.setField(author, "id", 1L);
		Book book = new Book("Effective Java", author, new BigDecimal("54.99"));
		ReflectionTestUtils.setField(book, "id", 1L);
		return book;
	}

	@Test
	void getBookById_returnsEnvelopeWithBook() throws Exception {
		when(bookService.getBookById(1L)).thenReturn(sampleBook());

		mockMvc.perform(get("/api/v1/books/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.title").value("Effective Java"))
				.andExpect(jsonPath("$.data.author.name").value("Joshua Bloch"));
	}

	@Test
	void getBookById_unknownId_returns404Envelope() throws Exception {
		when(bookService.getBookById(anyLong()))
				.thenThrow(ApiException.notFound("Book with id 999 not found"));

		mockMvc.perform(get("/api/v1/books/999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Book with id 999 not found"));
	}

	@Test
	void createBook_emptyBody_returns400WithFieldErrors() throws Exception {
		mockMvc.perform(post("/api/v1/books")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.errors.length()").value(3));

		verifyNoInteractions(bookService);
	}
}

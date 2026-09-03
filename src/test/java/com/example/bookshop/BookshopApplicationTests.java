/*
 ^ TUTORIAL 13 — level 3: the whole application (@SpringBootTest)

 ? @SpringBootTest starts the REAL application context: every bean,
 ? the real service, the real repositories, H2 with data.sql seeds.
 ? @AutoConfigureMockMvc adds MockMvc on top so we can drive it
 ? through HTTP semantics without opening a network port.

 ? contextLoads() looks empty but is not useless: it fails if ANY
 ? bean cannot start - bad config, missing dependency, broken
 ? entity mapping. The cheapest smoke alarm there is.

 ? What this level is FOR: "does the whole thing hang together" -
 ? a few end-to-end paths. It is the slowest level (starts the full
 ? app), so keep it thin: the pyramid is many unit tests, some slice
 ? tests, FEW full tests.
*/
package com.example.bookshop;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class BookshopApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	// TUTORIAL 15: security lives in the REAL filter chain, which the
	// @WebMvcTest slice does not enforce (verified: an unauthenticated
	// POST reached validation there). So the lock gets its regression
	// test HERE, at the full level.
	@Test
	void fullStack_writeWithoutToken_isRejectedWith401() throws Exception {
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.post("/api/v1/books")
				.contentType(org.springframework.http.MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false));
	}

	@Test
	void fullStack_listBooks_servesSeededData() throws Exception {
		mockMvc.perform(get("/api/v1/books?sort=id"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data[0].title").value("Effective Java"))
				.andExpect(jsonPath("$.data[0].author.name").value("Joshua Bloch"))
				.andExpect(jsonPath("$.meta.total").value(5));
	}
}

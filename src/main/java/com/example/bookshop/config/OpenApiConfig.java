/*
 ^ TUTORIAL 17 — describing the API to machines (and Swagger UI)

 ? OpenAPI is a STANDARD JSON description of an HTTP API: every
 ? path, parameter, request/response shape, status code. springdoc
 ? GENERATES it by reading our controllers, DTOs and validation
 ? annotations at runtime - the docs cannot drift from the code the
 ? way a hand-written wiki page does.
 ?   /v3/api-docs   the raw spec (import this URL into Postman)
 ?   /swagger-ui    a browsable, clickable UI on top of it

 ? This bean adds what springdoc cannot guess: the title, and the
 ? fact that protected endpoints want "Authorization: Bearer ..." -
 ? which gives Swagger UI its Authorize button (paste a token from
 ? POST /api/v1/auth/login and every "Try it out" sends it).
*/
package com.example.bookshop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI bookshopOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Bookshop API")
						.description("Course project and starter template. "
								+ "Same response envelope on every endpoint: "
								+ "{success, message, data, meta}.")
						.version("v1"))
				.components(new Components().addSecuritySchemes("bearerAuth",
						new SecurityScheme()
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")))
				.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
	}
}

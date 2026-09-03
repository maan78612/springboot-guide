/*
 ^ TUTORIAL 04 — reading configuration

 * First version of this class read settings one by one with
 * @Value("${bookshop.currency}"). That works, but every reader of a
 * setting repeats the key string, and a typo only explodes when THAT
 * class is created. See the tutorial doc for the comparison.
 + Final version: inject the one typed BookshopProperties bean, like
 +   any other dependency. Config is just another thing the container
 +   hands you.
*/
package com.example.bookshop.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bookshop.config.BookshopProperties;
import com.example.bookshop.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/shop")
public class ShopController {

	private final BookshopProperties properties;

	public ShopController(BookshopProperties properties) {
		this.properties = properties;
	}

	@GetMapping
	public ApiResponse<Map<String, String>> getShopInfo() {
		return ApiResponse.ok("Shop info fetched", Map.of(
				"name", properties.getShopName(),
				"currency", properties.getCurrency()));
	}
}

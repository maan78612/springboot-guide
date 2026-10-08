package com.example.bookshop.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bookshop.config.BookshopProperties;

/**
 * REST endpoint for returning the configured shop details.
 *
 * | Method | Endpoint      | Status | Description                |
 * |--------|---------------|--------|----------------------------|
 * | GET    | /api/v1/shop  | 200    | Return shop name/currency  |
 *
 * | Key                    | Explanation                           |
 * |------------------------|---------------------------------------|
 * | Constructor injection  | Provides the typed configuration      |
 */
@RestController
@RequestMapping("/api/v1/shop")
public class ShopController {

    private final BookshopProperties properties;

    public ShopController(BookshopProperties properties) {
        this.properties = properties;
    }

    @GetMapping
    public Map<String, String> getShopInfo() {
        return Map.of(
                "name", properties.getShopName(),
                "currency", properties.getCurrency()
        );
    }
}

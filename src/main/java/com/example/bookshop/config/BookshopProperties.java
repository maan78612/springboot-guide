package com.example.bookshop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed Java representation of the bookshop settings.
 *
 * | Key / Annotation         | Why we use it                                    |
 * |--------------------------|--------------------------------------------------|
 * | @ConfigurationProperties | Binds bookshop.* values to these fields          |
 * | shopName / currency       | Hold configured values used by the application  |
 * | getters and setters       | Allow Spring's property binder to read and write |
 */
@ConfigurationProperties(prefix = "bookshop")
public class BookshopProperties {

    private String shopName;
    private String currency;

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}

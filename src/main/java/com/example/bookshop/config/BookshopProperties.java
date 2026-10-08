package com.example.bookshop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed Java representation of the bookshop settings.
 *
 * | Key / Annotation         | Why we use it                                     |
 * |--------------------------|---------------------------------------------------|
 * | @ConfigurationProperties | Binds bookshop.* values to these fields           |
 * | shopName / currency      | Hold configured values used by the application   |
 * | Catalog                  | Groups page-size configuration under bookshop.*   |
 * | getters and setters      | Allow Spring's property binder to read and write  |
 */
@ConfigurationProperties(prefix = "bookshop")
public class BookshopProperties {

    private String shopName;
    private String currency;
    private final Catalog catalog = new Catalog();

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

    public Catalog getCatalog() {
        return catalog;
    }

    public static class Catalog {
        private int defaultPageSize = 10;
        private int maxPageSize = 100;

        public int getDefaultPageSize() {
            return defaultPageSize;
        }

        public void setDefaultPageSize(int defaultPageSize) {
            this.defaultPageSize = defaultPageSize;
        }

        public int getMaxPageSize() {
            return maxPageSize;
        }

        public void setMaxPageSize(int maxPageSize) {
            this.maxPageSize = maxPageSize;
        }
    }
}

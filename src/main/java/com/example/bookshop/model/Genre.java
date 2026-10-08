package com.example.bookshop.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Entity for a catalog genre.
 *
 * | Key                   | Why we use it                                   |
 * |-----------------------|-------------------------------------------------|
 * | @Entity               | Maps Genre objects to rows in the genre table   |
 * | @Id / @GeneratedValue | Marks the generated primary key                |
 * | no reverse collection | Only maps relationships the app needs to use   |
 */
@Entity
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    protected Genre() {
    }

    public Genre(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
